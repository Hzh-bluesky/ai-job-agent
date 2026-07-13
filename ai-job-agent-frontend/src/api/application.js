import request from './request'

export function createApplication(data) {
  return request.post('/applications', data).then((res) => res.data.data)
}

export function getApplicationPage(params) {
  return request.get('/applications', { params }).then((res) => res.data.data)
}

export function getApplicationDetail(id) {
  return request.get(`/applications/${id}`).then((res) => res.data.data)
}

export function updateApplication(id, data) {
  return request.put(`/applications/${id}`, data).then((res) => res.data.data)
}

export function updateApplicationStatus(id, data) {
  return request.put(`/applications/${id}/status`, data).then((res) => res.data.data)
}

export function deleteApplication(id) {
  return request.delete(`/applications/${id}`).then((res) => res.data.data)
}
