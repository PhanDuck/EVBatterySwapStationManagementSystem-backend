import React, { useState } from 'react'
import api from '../api'

export default function SwapByCode(){
  const [code, setCode] = useState('')
  const [oldInfo, setOldInfo] = useState(null)
  const [newInfo, setNewInfo] = useState(null)
  const [message, setMessage] = useState(null)

  async function fetchOld(){
    try{
      const r = await api.get(`/swap-transaction/old-battery?code=${encodeURIComponent(code)}`)
      setOldInfo(r.data)
    }catch(e){
      setOldInfo(null)
      setMessage('Error fetching old battery: '+ (e.response?.data || e.message))
    }
  }

  async function fetchNew(){
    try{
      const r = await api.get(`/swap-transaction/new-battery?code=${encodeURIComponent(code)}`)
      setNewInfo(r.data)
    }catch(e){
      setNewInfo(null)
      setMessage('Error fetching new battery: '+ (e.response?.data || e.message))
    }
  }

  async function doSwap(){
    try{
      const r = await api.post(`/swap-transaction/swap-by-code?code=${encodeURIComponent(code)}`)
      setMessage('Swap created: ' + r.data.id)
    }catch(e){
      setMessage('Swap failed: ' + (e.response?.data || e.message))
    }
  }

  return (
    <div style={{maxWidth:600}}>
      <h2>Swap by Confirmation Code</h2>
      <div>
        <input placeholder="confirmation code" value={code} onChange={e=>setCode(e.target.value)} />
        <button onClick={fetchOld} style={{marginLeft:5}}>Old</button>
        <button onClick={fetchNew} style={{marginLeft:5}}>New</button>
        <button onClick={doSwap} style={{marginLeft:5}}>Swap</button>
      </div>

      {oldInfo && <div style={{marginTop:10}}>
        <h4>Old Battery</h4>
        <pre>{JSON.stringify(oldInfo, null, 2)}</pre>
      </div>}

      {newInfo && <div style={{marginTop:10}}>
        <h4>New Battery</h4>
        <pre>{JSON.stringify(newInfo, null, 2)}</pre>
      </div>}

      {message && <div style={{marginTop:10}}>{message}</div>}
    </div>
  )
}
