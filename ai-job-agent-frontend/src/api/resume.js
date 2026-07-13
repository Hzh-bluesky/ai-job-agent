import request from './request'

export function getResumePage(params) {
  return request.get('/resumes', { params }).then((res) => res.data.data)
}

export function getResumeDetail(id) {
  return request.get(`/resumes/${id}`).then((res) => res.data.data)
}

export function createResume(data) {
  return request.post('/resumes', data).then((res) => res.data.data)
}

export function updateResume(id, data) {
  return request.put(`/resumes/${id}`, data).then((res) => res.data.data)
}

export function setDefaultResume(id) {
  return request.put(`/resumes/${id}/default`).then((res) => res.data.data)
}

export function deleteResume(id) {
  return request.delete(`/resumes/${id}`).then((res) => res.data.data)
}
