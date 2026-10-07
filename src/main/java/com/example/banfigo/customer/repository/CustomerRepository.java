package com.example.banfigo.customer.repository;

import com.example.banfigo.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer, Long> {

}
