import request from './request'

export function generateApplyPlan(data) {
  const payload = {
    resumeId: Number(data.resumeId),
    jobPostId: Number(data.jobPostId),
    forceRegenerate: data.forceRegenerate === true
  }
  return request.post('/apply-plans/generate', payload, { timeout: 120000 }).then((res) => res.data.data)
}

export function getApplyPlanPage(params) {
  return request.get('/apply-plans', { params }).then((res) => res.data.data)
}

export function getApplyPlanDetail(id) {
  return request.get(`/apply-plans/${id}`).then((res) => res.data.data)
}
