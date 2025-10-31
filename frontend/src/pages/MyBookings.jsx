import React, { useEffect, useState } from 'react'
import api from '../api'

export default function MyBookings(){
  const [bookings, setBookings] = useState([])
  const [error, setError] = useState(null)

  useEffect(()=>{
    api.get('/booking/my-bookings')
      .then(r=>setBookings(r.data))
      .catch(e=>setError(e.message))
  },[])

  async function cancel(id){
    try{
      await api.patch(`/booking/my-bookings/${id}/cancel`)
      setBookings(b => b.filter(x=>x.id!==id))
    }catch(e){
      alert('Cancel failed: ' + (e.response?.data || e.message))
    }
  }

  return (
    <div>
      <h2>My Bookings</h2>
      {error && <div style={{color:'red'}}>{error}</div>}
      <ul>
        {bookings.map(b=> (
          <li key={b.id}>{b.station?.name} - {b.status}
            {b.status==='PENDING' && <button onClick={()=>cancel(b.id)} style={{marginLeft:10}}>Cancel</button>}
          </li>
        ))}
      </ul>
    </div>
  )
}
