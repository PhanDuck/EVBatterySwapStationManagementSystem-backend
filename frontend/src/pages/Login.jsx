import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'

export default function Login(){
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  async function submit(e){
    e.preventDefault()
    try{
      const res = await api.post('/login', { phone, password })
      const user = res.data
      localStorage.setItem('token', user.token)
      localStorage.setItem('user', JSON.stringify(user))
      navigate('/')
    }catch(err){
      setError(err.response?.data || err.message)
    }
  }

  return (
    <div style={{maxWidth:400}}>
      <h2>Login</h2>
      <form onSubmit={submit}>
        <div>
          <label>Phone</label>
          <input value={phone} onChange={e=>setPhone(e.target.value)} />
        </div>
        <div>
          <label>Password</label>
          <input type="password" value={password} onChange={e=>setPassword(e.target.value)} />
        </div>
        <button type="submit">Login</button>
      </form>
      {error && <div style={{color:'red'}}>{JSON.stringify(error)}</div>}
    </div>
  )
}
