import request from './request'

export function createResumeRewrite(data) {
  return request.post('/resume-rewrite', data).then((res) => res.data.data)
}

export function getResumeRewritePage(params) {
  return request.get('/resume-rewrite', { params }).then((res) => res.data.data)
}

export function getResumeRewriteDetail(id) {
  return request.get(`/resume-rewrite/${id}`).then((res) => res.data.data)
}

export function deleteResumeRewrite(id) {
  return request.delete(`/resume-rewrite/${id}`).then((res) => res.data.data)
}
