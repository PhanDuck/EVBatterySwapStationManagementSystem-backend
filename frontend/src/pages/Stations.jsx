import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'

export default function Stations(){
  const [stations, setStations] = useState([])
  const [error, setError] = useState(null)

  useEffect(()=>{
    api.get('/station')
      .then(r=>setStations(r.data))
      .catch(e=>setError(e.message))
  },[])

  return (
    <div>
      <h2>Stations</h2>
      {error && <div style={{color:'red'}}>{error}</div>}
      <ul>
        {stations.map(s=> (
          <li key={s.id}>
            <Link to={`/station/${s.id}`}>{s.name} ({s.city})</Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
