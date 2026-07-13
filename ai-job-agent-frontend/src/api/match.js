import request from './request'

export function createMatchReport(data) {
  return request.post('/match', data).then((res) => res.data.data)
}

export function getMatchPage(params) {
  return request.get('/match', { params }).then((res) => res.data.data)
}

export function getMatchDetail(id) {
  return request.get(`/match/${id}`).then((res) => res.data.data)
}

export function deleteMatchReport(id) {
  return request.delete(`/match/${id}`).then((res) => res.data.data)
}
