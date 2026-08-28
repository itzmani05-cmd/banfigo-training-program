package com.example.week1_backend_assesment.service;
import org.springframework.stereotype.Service;
import com.example.week1_backend_assesment.dto.CustomerRequest;
import com.example.week1_backend_assesment.dto.CustomerResponse;
import com.example.week1_backend_assesment.entity.Customer;
import com.example.week1_backend_assesment.repository.CustomerRepository;
import java.util.List;
import java.util.stream.Collectors;
import com.example.week1_backend_assesment.exception.ResourceNotFoundException;
@Service
public class CustomerService {
    private final CustomerRepository customerRespository;
    public CustomerService(CustomerRepository customerRespository){
        this.customerRespository = customerRespository;
    }
    public CustomerResponse createCustomer(CustomerRequest request){
        Customer customer=new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        Customer savedCustomer=customerRespository.save(customer);
        return mapToResponse(savedCustomer);
    }
    public List<CustomerResponse> getAllCustomers(){
        return customerRespository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    public CustomerResponse getCustomerById(Long id){
        Customer customer =customerRespository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToResponse(customer);
    }
    public CustomerResponse updateCustomer(Long id, CustomerRequest request){
        Customer customer=customerRespository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        Customer updatedcustomer= customerRespository.save(customer);
        return mapToResponse(updatedcustomer);
    }
    public boolean deleteCustomer(Long id){
        Customer customer=customerRespository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customerRespository.delete(customer);
        return true;
    }

    private CustomerResponse mapToResponse(Customer customer){
        return new CustomerResponse(
            customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone(), customer.getAddress()
        );
    }
}
