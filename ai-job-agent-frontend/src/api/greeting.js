import request from './request'

export function generateGreeting(data) {
  return request.post('/greetings/generate', data).then((res) => res.data.data)
}

export function getGreetingPage(params) {
  return request.get('/greetings', { params }).then((res) => res.data.data)
}

export function getGreetingDetail(id) {
  return request.get(`/greetings/${id}`).then((res) => res.data.data)
}

export function deleteGreeting(id) {
  return request.delete(`/greetings/${id}`).then((res) => res.data.data)
}
