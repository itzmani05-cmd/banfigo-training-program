package com.example.week1_backend_assesment.repository;

import com.example.week1_backend_assesment.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
public class CustomerRepository extends JpaRepository<Customer, Long> {

}
