import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api'

export default function StationDetail(){
  const { id } = useParams()
  const [station, setStation] = useState(null)
  const [batteries, setBatteries] = useState([])
  const [error, setError] = useState(null)

  useEffect(()=>{
    api.get(`/station/${id}`)
      .then(r=>setStation(r.data))
      .catch(e=>setError(e.message))

    api.get(`/station/${id}/batteries`)
      .then(r=>setBatteries(r.data))
      .catch(e=>console.warn(e))
  },[id])

  if (error) return <div style={{color:'red'}}>{error}</div>
  if (!station) return <div>Loading...</div>

  return (
    <div>
      <h2>{station.name}</h2>
      <p>{station.address}</p>
      <h3>Batteries</h3>
      <ul>
        {batteries.map(b=> <li key={b.id}>{b.code} - SOH: {b.soh}</li>)}
      </ul>
    </div>
  )
}
