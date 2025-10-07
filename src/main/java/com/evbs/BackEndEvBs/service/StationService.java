package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.model.request.CreateStationRequest;
import com.evbs.BackEndEvBs.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    @Autowired
    StationRepository stationRepository;

    // READ - Lấy tất cả trạm (Public)
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    // READ - Lấy trạm theo ID (Public)
    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new AuthenticationException("Station not found"));
    }

    public CreateStationRequest createStation(CreateStationRequest request) {
        if (stationRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Station name already exists!");
        }

        Station station = new Station();
        station.setName(request.getName());
        station.setLocation(request.getLocation());
        station.setCapacity(request.getCapacity());
        station.setContactInfo(request.getContactInfo());
        station.setLatitude(request.getLatitude());
        station.setLongitude(request.getLongitude());
        station.setStatus(request.getStatus());

        stationRepository.save(station);

        // Trả về request như user muốn
        return request;
    }
}
