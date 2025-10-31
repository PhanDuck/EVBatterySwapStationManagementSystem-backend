import React from 'react'
import { Routes, Route, Link, useNavigate } from 'react-router-dom'
import Login from './pages/Login'
import Stations from './pages/Stations'
import StationDetail from './pages/StationDetail'
import BookingCreate from './pages/BookingCreate'
import MyBookings from './pages/MyBookings'
import SwapByCode from './pages/SwapByCode'
import api from './api'

function Nav() {
  const navigate = useNavigate()
  const token = localStorage.getItem('token')

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    navigate('/login')
  }

  return (
    <nav style={{padding:10, borderBottom:'1px solid #ddd'}}>
      <Link to="/" style={{marginRight:10}}>Stations</Link>
      <Link to="/book" style={{marginRight:10}}>Create Booking</Link>
      <Link to="/my-bookings" style={{marginRight:10}}>My Bookings</Link>
      <Link to="/swap" style={{marginRight:10}}>Swap by Code</Link>
      {token ? (
        <button onClick={logout}>Logout</button>
      ) : (
        <Link to="/login" style={{marginLeft:10}}>Login</Link>
      )}
    </nav>
  )
}

export default function App(){
  return (
    <div>
      <Nav />
      <div style={{Padding:20}}>
        <Routes>
          <Route path="/" element={<Stations/>} />
          <Route path="/station/:id" element={<StationDetail/>} />
          <Route path="/login" element={<Login/>} />
          <Route path="/book" element={<BookingCreate/>} />
          <Route path="/my-bookings" element={<MyBookings/>} />
          <Route path="/swap" element={<SwapByCode/>} />
        </Routes>
      </div>
    </div>
  )
}
