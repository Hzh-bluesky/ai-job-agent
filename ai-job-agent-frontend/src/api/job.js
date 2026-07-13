import request from './request'

export function analyzeJob(data) {
  return request.post('/jobs/analyze', data).then((res) => res.data.data)
}

export function importJobs(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request
    .post('/jobs/import', formData, {
      timeout: 120000,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    .then((res) => res.data.data)
}

export function getJobPage(params) {
  return request.get('/jobs', { params }).then((res) => res.data.data)
}

export function getJobDetail(id) {
  return request.get(`/jobs/${id}`).then((res) => res.data.data)
}

export function deleteJob(id) {
  return request.delete(`/jobs/${id}`).then((res) => res.data.data)
}
