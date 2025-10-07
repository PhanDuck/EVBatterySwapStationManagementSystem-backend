package com.evbs.BackEndEvBs.repository;


import com.evbs.BackEndEvBs.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthenticationRepository extends JpaRepository<User, Long> {
        //tìm user thông qua phone
    User findUserByPhoneNumber(String phoneNumber);

    User findUserById(long id);

}
