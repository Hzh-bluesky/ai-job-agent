import request from './request'

export function generateInterviewQuestions(data) {
  return request.post('/interviews/generate', data).then((res) => res.data.data)
}

export function getInterviewPage(params) {
  return request.get('/interviews', { params }).then((res) => res.data.data)
}

export function getInterviewDetail(id) {
  return request.get(`/interviews/${id}`).then((res) => res.data.data)
}

export function deleteInterviewRecord(id) {
  return request.delete(`/interviews/${id}`).then((res) => res.data.data)
}
