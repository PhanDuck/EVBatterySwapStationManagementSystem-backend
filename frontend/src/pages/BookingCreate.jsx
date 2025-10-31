import React, { useEffect, useState } from 'react'
import api from '../api'
import { useNavigate } from 'react-router-dom'

export default function BookingCreate(){
  const [vehicles, setVehicles] = useState([])
  const [vehicleId, setVehicleId] = useState('')
  const [stationId, setStationId] = useState('')
  const [stations, setStations] = useState([])
  const [message, setMessage] = useState(null)
  const navigate = useNavigate()

  useEffect(()=>{
    api.get('/vehicle/my-vehicles')
      .then(r=>setVehicles(r.data))
      .catch(()=>setVehicles([]))

    api.get('/station')
      .then(r=>setStations(r.data))
      .catch(()=>setStations([]))
  },[])

  async function submit(e){
    e.preventDefault()
    try{
      const res = await api.post('/booking', { vehicleId: Number(vehicleId), stationId: Number(stationId) })
      setMessage('Booking created: '+ res.data.id)
      navigate('/my-bookings')
    }catch(err){
      setMessage('Error: ' + (err.response?.data || err.message))
    }
  }

  return (
    <div style={{maxWidth:600}}>
      <h2>Create Booking</h2>
      <form onSubmit={submit}>
        <div>
          <label>Vehicle</label>
          <select value={vehicleId} onChange={e=>setVehicleId(e.target.value)}>
            <option value="">-- choose --</option>
            {vehicles.map(v=> <option key={v.id} value={v.id}>{v.plate} ({v.batteryType?.name})</option>)}
          </select>
        </div>
        <div>
          <label>Station</label>
          <select value={stationId} onChange={e=>setStationId(e.target.value)}>
            <option value="">-- choose --</option>
            {stations.map(s=> <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <button type="submit">Create</button>
      </form>
      {message && <div style={{marginTop:10}}>{message}</div>}
    </div>
  )
}
