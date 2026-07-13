package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.jobagent.agent.JobAgent;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.common.PageResult;
import com.example.jobagent.dto.JobAnalyzeDTO;
import com.example.jobagent.dto.JobPageQueryDTO;
import com.example.jobagent.entity.JobAnalysis;
import com.example.jobagent.entity.JobPost;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.JobAnalysisMapper;
import com.example.jobagent.mapper.JobPostMapper;
import com.example.jobagent.service.JobService;
import com.example.jobagent.service.KnowledgeService;
import com.example.jobagent.vo.JobAnalysisVO;
import com.example.jobagent.vo.JobImportItemVO;
import com.example.jobagent.vo.JobImportResultVO;
import com.example.jobagent.vo.JobPostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobPostMapper jobPostMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final JobAgent jobAgent;
    private final KnowledgeService knowledgeService;

    private static final int MAX_IMPORT_ROWS = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobAnalysisVO analyze(Long userId, JobAnalyzeDTO analyzeDTO) {
        JobPost jobPost = new JobPost();
        jobPost.setUserId(userId);
        jobPost.setJdText(analyzeDTO.getJdText());
        jobPost.setSource(analyzeDTO.getSource());
        jobPostMapper.insert(jobPost);

        JobAnalysis analysis = jobAgent.parseJob(userId, analyzeDTO.getJdText());
        analysis.setUserId(userId);
        analysis.setJobPostId(jobPost.getId());
        jobAnalysisMapper.insert(analysis);

        jobPost.setCompanyName(analysis.getCompanyName());
        jobPost.setJobName(analysis.getJobName());
        jobPost.setCity(analysis.getCity());
        jobPost.setSalary(analysis.getSalary());
        jobPostMapper.updateById(jobPost);
        indexJobAnalysisKnowledge(jobPost, analysis);

        return toAnalysisVO(analysis);
    }

    @Override
    public JobImportResultVO importJobs(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传 Excel 或 CSV 文件");
        }

        List<ImportedJobRow> rows = parseImportFile(file);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件中没有可导入的岗位数据");
        }
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次最多导入" + MAX_IMPORT_ROWS + "条岗位");
        }

        List<JobImportItemVO> items = new ArrayList<>();
        Set<String> fileDuplicateKeys = new LinkedHashSet<>();
        for (ImportedJobRow row : rows) {
            try {
                validateImportRow(row);
                String duplicateKey = buildDuplicateKey(row.companyName(), row.jobName(), row.city());
                if (!fileDuplicateKeys.add(duplicateKey)) {
                    items.add(failedImportItem(row, "文件内存在重复岗位"));
                    continue;
                }
                if (existsDuplicateJob(userId, row)) {
                    items.add(failedImportItem(row, "该岗位已存在，请勿重复导入"));
                    continue;
                }
                items.add(importSingleJob(userId, row));
            } catch (BusinessException e) {
                items.add(failedImportItem(row, e.getMessage()));
            } catch (Exception e) {
                log.warn("[JobImport] row import failed userId={}, rowNumber={}", userId, row.rowNumber(), e);
                items.add(failedImportItem(row, "导入失败：" + e.getMessage()));
            }
        }

        int successCount = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getSuccess())).count();
        return JobImportResultVO.builder()
                .totalCount(items.size())
                .successCount(successCount)
                .failureCount(items.size() - successCount)
                .items(items)
                .build();
    }

    @Override
    public PageResult<JobPostVO> page(Long userId, JobPageQueryDTO queryDTO) {
        LambdaQueryWrapper<JobPost> queryWrapper = new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getUserId, userId)
                .orderByDesc(JobPost::getUpdateTime);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(JobPost::getCompanyName, keyword)
                    .or()
                    .like(JobPost::getJobName, keyword)
                    .or()
                    .like(JobPost::getCity, keyword)
                    .or()
                    .like(JobPost::getJdText, keyword));
        }

        Page<JobPost> page = jobPostMapper.selectPage(
                new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()),
                queryWrapper
        );
        List<JobPostVO> records = page.getRecords().stream()
                .map(jobPost -> toJobPostVO(jobPost, null))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public JobPostVO getDetail(Long userId, Long id) {
        JobPost jobPost = getOwnedJobPost(userId, id);
        JobAnalysis analysis = getJobAnalysis(userId, id);
        return toJobPostVO(jobPost, analysis == null ? null : toAnalysisVO(analysis));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedJobPost(userId, id);

        jobAnalysisMapper.delete(new LambdaQueryWrapper<JobAnalysis>()
                .eq(JobAnalysis::getUserId, userId)
                .eq(JobAnalysis::getJobPostId, id));

        int rows = jobPostMapper.delete(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, id)
                .eq(JobPost::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在或无权访问");
        }
    }

    private List<ImportedJobRow> parseImportFile(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".csv")) {
                return parseCsv(file);
            }
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(file);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[JobImport] parse file failed filename={}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件解析失败，请检查模板格式");
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 .xlsx、.xls、.csv 文件");
    }

    private List<ImportedJobRow> parseExcel(MultipartFile file) throws Exception {
        List<ImportedJobRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return rows;
            }
            Map<String, Integer> headerIndex = readExcelHeader(sheet.getRow(0), formatter);
            validateHeader(headerIndex);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                ImportedJobRow imported = new ImportedJobRow(
                        i + 1,
                        cellValue(row, headerIndex, "companyName", formatter),
                        cellValue(row, headerIndex, "jobName", formatter),
                        cellValue(row, headerIndex, "city", formatter),
                        cellValue(row, headerIndex, "salary", formatter),
                        cellValue(row, headerIndex, "jdText", formatter),
                        cellValue(row, headerIndex, "sourceLink", formatter),
                        cellValue(row, headerIndex, "source", formatter)
                );
                if (!isBlankRow(imported)) {
                    rows.add(imported);
                }
            }
        }
        return rows;
    }

    private List<ImportedJobRow> parseCsv(MultipartFile file) throws Exception {
        List<ImportedJobRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                return rows;
            }
            Map<String, Integer> headerIndex = readCsvHeader(stripBom(headerLine));
            validateHeader(headerIndex);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                List<String> values = splitCsvLine(line);
                ImportedJobRow imported = new ImportedJobRow(
                        rowNumber,
                        valueAt(values, headerIndex, "companyName"),
                        valueAt(values, headerIndex, "jobName"),
                        valueAt(values, headerIndex, "city"),
                        valueAt(values, headerIndex, "salary"),
                        valueAt(values, headerIndex, "jdText"),
                        valueAt(values, headerIndex, "sourceLink"),
                        valueAt(values, headerIndex, "source")
                );
                if (!isBlankRow(imported)) {
                    rows.add(imported);
                }
            }
        }
        return rows;
    }

    private Map<String, Integer> readExcelHeader(Row row, DataFormatter formatter) {
        Map<String, Integer> headerIndex = new HashMap<>();
        if (row == null) {
            return headerIndex;
        }
        for (Cell cell : row) {
            String normalized = normalizeHeader(formatter.formatCellValue(cell));
            String fieldName = mapHeaderToField(normalized);
            if (fieldName != null) {
                headerIndex.put(fieldName, cell.getColumnIndex());
            }
        }
        return headerIndex;
    }

    private Map<String, Integer> readCsvHeader(String headerLine) {
        Map<String, Integer> headerIndex = new HashMap<>();
        List<String> headers = splitCsvLine(headerLine);
        for (int i = 0; i < headers.size(); i++) {
            String fieldName = mapHeaderToField(normalizeHeader(headers.get(i)));
            if (fieldName != null) {
                headerIndex.put(fieldName, i);
            }
        }
        return headerIndex;
    }

    private void validateHeader(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("companyName")
                || !headerIndex.containsKey("jobName")
                || !headerIndex.containsKey("city")
                || !headerIndex.containsKey("salary")
                || !headerIndex.containsKey("jdText")
                || !headerIndex.containsKey("sourceLink")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板表头必须包含：公司名称、岗位名称、城市、薪资、JD、来源链接");
        }
    }

    private String mapHeaderToField(String header) {
        return switch (header) {
            case "公司", "公司名称", "company", "companyname" -> "companyName";
            case "岗位", "岗位名称", "职位", "职位名称", "job", "jobname", "position" -> "jobName";
            case "城市", "city" -> "city";
            case "薪资", "salary" -> "salary";
            case "jd", "岗位jd", "岗位描述", "岗位原文", "jdtext", "description" -> "jdText";
            case "来源链接", "链接", "岗位链接", "sourcelink", "sourceurl", "url", "link" -> "sourceLink";
            case "来源", "平台", "source" -> "source";
            default -> null;
        };
    }

    private String normalizeHeader(String value) {
        return stripBom(defaultIfBlank(value, ""))
                .trim()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
    }

    private String cellValue(Row row, Map<String, Integer> headerIndex, String fieldName, DataFormatter formatter) {
        Integer index = headerIndex.get(fieldName);
        if (index == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String valueAt(List<String> values, Map<String, Integer> headerIndex, String fieldName) {
        Integer index = headerIndex.get(fieldName);
        if (index == null || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private void validateImportRow(ImportedJobRow row) {
        if (!StringUtils.hasText(row.companyName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公司名称不能为空");
        }
        if (!StringUtils.hasText(row.jobName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位名称不能为空");
        }
        if (!StringUtils.hasText(row.city())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "城市不能为空");
        }
        if (!StringUtils.hasText(row.salary())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "薪资不能为空");
        }
        if (!StringUtils.hasText(row.jdText())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "JD不能为空");
        }
        if (!StringUtils.hasText(row.sourceLink())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "来源链接不能为空");
        }
        if (row.jdText().trim().length() < 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "JD内容过短，请填写完整岗位描述");
        }
        if (!isValidUrl(row.sourceLink())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "来源链接格式不正确");
        }
    }

    private boolean existsDuplicateJob(Long userId, ImportedJobRow row) {
        Long linkCount = jobPostMapper.selectCount(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getUserId, userId)
                .eq(JobPost::getSourceLink, row.sourceLink().trim()));
        if (linkCount > 0) {
            return true;
        }

        Long titleCount = jobPostMapper.selectCount(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getUserId, userId)
                .eq(JobPost::getCompanyName, row.companyName().trim())
                .eq(JobPost::getJobName, row.jobName().trim())
                .eq(JobPost::getCity, row.city().trim()));
        if (titleCount > 0) {
            return true;
        }

        Long jdCount = jobPostMapper.selectCount(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getUserId, userId)
                .eq(JobPost::getJdText, row.jdText().trim()));
        return jdCount > 0;
    }

    private JobImportItemVO importSingleJob(Long userId, ImportedJobRow row) {
        JobAnalysis analysis = jobAgent.parseJob(userId, row.jdText().trim());
        JobPost jobPost = new JobPost();
        Long jobPostId = null;
        try {
            jobPost.setUserId(userId);
            jobPost.setJdText(row.jdText().trim());
            jobPost.setSource(defaultIfBlank(row.source(), "批量导入"));
            jobPost.setSourceLink(row.sourceLink().trim());
            jobPost.setCompanyName(row.companyName().trim());
            jobPost.setJobName(row.jobName().trim());
            jobPost.setCity(row.city().trim());
            jobPost.setSalary(row.salary().trim());
            jobPostMapper.insert(jobPost);
            jobPostId = jobPost.getId();

            analysis.setUserId(userId);
            analysis.setJobPostId(jobPost.getId());
            analysis.setCompanyName(defaultIfBlank(analysis.getCompanyName(), row.companyName().trim()));
            analysis.setJobName(defaultIfBlank(analysis.getJobName(), row.jobName().trim()));
            analysis.setCity(defaultIfBlank(analysis.getCity(), row.city().trim()));
            analysis.setSalary(defaultIfBlank(analysis.getSalary(), row.salary().trim()));
            jobAnalysisMapper.insert(analysis);

            fillMissingJobPostFactsFromAnalysis(jobPost, analysis);
            jobPostMapper.updateById(jobPost);
            indexJobAnalysisKnowledge(jobPost, analysis);

            return JobImportItemVO.builder()
                    .rowNumber(row.rowNumber())
                    .success(true)
                    .jobPostId(jobPost.getId())
                    .jobAnalysisId(analysis.getId())
                    .companyName(jobPost.getCompanyName())
                    .jobName(jobPost.getJobName())
                    .city(jobPost.getCity())
                    .salary(jobPost.getSalary())
                    .sourceLink(jobPost.getSourceLink())
                    .build();
        } catch (Exception e) {
            if (jobPostId != null) {
                jobAnalysisMapper.delete(new LambdaQueryWrapper<JobAnalysis>()
                        .eq(JobAnalysis::getUserId, userId)
                        .eq(JobAnalysis::getJobPostId, jobPostId));
                jobPostMapper.delete(new LambdaQueryWrapper<JobPost>()
                        .eq(JobPost::getUserId, userId)
                        .eq(JobPost::getId, jobPostId));
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "岗位导入失败：" + e.getMessage());
        }
    }

    private JobImportItemVO failedImportItem(ImportedJobRow row, String reason) {
        return JobImportItemVO.builder()
                .rowNumber(row.rowNumber())
                .success(false)
                .failureReason(reason)
                .companyName(row.companyName())
                .jobName(row.jobName())
                .city(row.city())
                .salary(row.salary())
                .sourceLink(row.sourceLink())
                .build();
    }

    private void fillMissingJobPostFactsFromAnalysis(JobPost jobPost, JobAnalysis analysis) {
        if (!StringUtils.hasText(jobPost.getCompanyName())) {
            jobPost.setCompanyName(analysis.getCompanyName());
        }
        if (!StringUtils.hasText(jobPost.getJobName())) {
            jobPost.setJobName(analysis.getJobName());
        }
        if (!StringUtils.hasText(jobPost.getCity())) {
            jobPost.setCity(analysis.getCity());
        }
        if (!StringUtils.hasText(jobPost.getSalary())) {
            jobPost.setSalary(analysis.getSalary());
        }
    }

    private boolean isBlankRow(ImportedJobRow row) {
        return !StringUtils.hasText(row.companyName())
                && !StringUtils.hasText(row.jobName())
                && !StringUtils.hasText(row.city())
                && !StringUtils.hasText(row.salary())
                && !StringUtils.hasText(row.jdText())
                && !StringUtils.hasText(row.sourceLink());
    }

    private String buildDuplicateKey(String companyName, String jobName, String city) {
        return normalizeValue(companyName) + "|" + normalizeValue(jobName) + "|" + normalizeValue(city);
    }

    private String normalizeValue(String value) {
        return defaultIfBlank(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidUrl(String value) {
        String url = value.trim().toLowerCase(Locale.ROOT);
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private JobPost getOwnedJobPost(Long userId, Long id) {
        JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, id)
                .eq(JobPost::getUserId, userId));
        if (jobPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在或无权访问");
        }
        return jobPost;
    }

    private JobAnalysis getJobAnalysis(Long userId, Long jobPostId) {
        return jobAnalysisMapper.selectOne(new LambdaQueryWrapper<JobAnalysis>()
                .eq(JobAnalysis::getUserId, userId)
                .eq(JobAnalysis::getJobPostId, jobPostId)
                .orderByDesc(JobAnalysis::getId)
                .last("LIMIT 1"));
    }

    private JobPostVO toJobPostVO(JobPost jobPost, JobAnalysisVO analysisVO) {
        return JobPostVO.builder()
                .id(jobPost.getId())
                .userId(jobPost.getUserId())
                .jdText(jobPost.getJdText())
                .source(jobPost.getSource())
                .sourceLink(jobPost.getSourceLink())
                .companyName(jobPost.getCompanyName())
                .jobName(jobPost.getJobName())
                .city(jobPost.getCity())
                .salary(jobPost.getSalary())
                .analysis(analysisVO)
                .createTime(jobPost.getCreateTime())
                .updateTime(jobPost.getUpdateTime())
                .build();
    }

    private JobAnalysisVO toAnalysisVO(JobAnalysis analysis) {
        return JobAnalysisVO.builder()
                .id(analysis.getId())
                .userId(analysis.getUserId())
                .jobPostId(analysis.getJobPostId())
                .companyName(analysis.getCompanyName())
                .jobName(analysis.getJobName())
                .city(analysis.getCity())
                .salary(analysis.getSalary())
                .education(analysis.getEducation())
                .internshipCycle(analysis.getInternshipCycle())
                .techStack(analysis.getTechStack())
                .responsibilities(analysis.getResponsibilities())
                .requirements(analysis.getRequirements())
                .bonusPoints(analysis.getBonusPoints())
                .riskPoints(analysis.getRiskPoints())
                .rawResult(analysis.getRawResult())
                .createTime(analysis.getCreateTime())
                .updateTime(analysis.getUpdateTime())
                .build();
    }

    private void indexJobAnalysisKnowledge(JobPost jobPost, JobAnalysis analysis) {
        if (jobPost == null || analysis == null) {
            return;
        }
        try {
            knowledgeService.indexKnowledge(
                    analysis.getUserId(),
                    "JOB_ANALYSIS",
                    analysis.getId(),
                    defaultIfBlank(analysis.getCompanyName(), "") + " " + defaultIfBlank(analysis.getJobName(), "Job Analysis"),
                    buildJobAnalysisKnowledgeContent(jobPost, analysis)
            );
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] job analysis index failed userId={}, jobPostId={}, jobAnalysisId={}",
                    analysis.getUserId(), jobPost.getId(), analysis.getId(), e);
        }
    }

    private String buildJobAnalysisKnowledgeContent(JobPost jobPost, JobAnalysis analysis) {
        return "公司：" + defaultIfBlank(analysis.getCompanyName(), jobPost.getCompanyName()) + "\n"
                + "岗位：" + defaultIfBlank(analysis.getJobName(), jobPost.getJobName()) + "\n"
                + "城市：" + defaultIfBlank(analysis.getCity(), jobPost.getCity()) + "\n"
                + "薪资：" + defaultIfBlank(analysis.getSalary(), jobPost.getSalary()) + "\n"
                + "学历：" + defaultIfBlank(analysis.getEducation(), "") + "\n"
                + "实习周期：" + defaultIfBlank(analysis.getInternshipCycle(), "") + "\n"
                + "技术栈：" + defaultIfBlank(analysis.getTechStack(), "") + "\n"
                + "职责：" + defaultIfBlank(analysis.getResponsibilities(), "") + "\n"
                + "要求：" + defaultIfBlank(analysis.getRequirements(), "") + "\n"
                + "加分项：" + defaultIfBlank(analysis.getBonusPoints(), "") + "\n"
                + "风险点：" + defaultIfBlank(analysis.getRiskPoints(), "") + "\n"
                + "来源链接：" + defaultIfBlank(jobPost.getSourceLink(), "") + "\n"
                + "JD：" + defaultIfBlank(jobPost.getJdText(), "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private record ImportedJobRow(
            Integer rowNumber,
            String companyName,
            String jobName,
            String city,
            String salary,
            String jdText,
            String sourceLink,
            String source
    ) {
    }
}
