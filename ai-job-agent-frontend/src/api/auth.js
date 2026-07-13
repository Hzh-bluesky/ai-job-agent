import request from './request'

export function login(data) {
  return request.post('/auth/login', data).then((res) => res.data.data)
}
