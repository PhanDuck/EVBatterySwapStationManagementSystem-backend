package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    boolean existsByName(String name);
}