package com.example.banfigo.customer.service;
import org.springframework.stereotype.Service;
import com.example.banfigo.account.repository.BankAccountRepository;
import com.example.banfigo.beneficiary.repository.BeneficiaryRepository;
import com.example.banfigo.common.exception.ResourceNotFoundException;
import com.example.banfigo.consent.repository.ConsentRepository;
import com.example.banfigo.customer.dto.CustomerRequest;
import com.example.banfigo.customer.dto.CustomerResponse;
import com.example.banfigo.customer.entity.Customer;
import com.example.banfigo.customer.repository.CustomerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class CustomerService {
    private final CustomerRepository customerRespository;
    private final BankAccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ConsentRepository consentRepository;
    private final CurrentUser currentUser;
    public CustomerService(CustomerRepository customerRespository,
                           BankAccountRepository accountRepository,
                           BeneficiaryRepository beneficiaryRepository,
                           ConsentRepository consentRepository,
                           CurrentUser currentUser){
        this.customerRespository = customerRespository;
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.consentRepository = consentRepository;
        this.currentUser = currentUser;
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
    public CustomerResponse getCurrentCustomer(){
        return mapToResponse(currentUser.customer());
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
        requireNoLinkedRecords(customer);
        customerRespository.delete(customer);
        return true;
    }

    // Accounts, beneficiaries and consents reference the customer, so deleting it would fail on the
    // foreign keys (a 500). Refuse up front with a 409 that says what has to go first.
    private void requireNoLinkedRecords(Customer customer){
        List<String> linked = new ArrayList<>();
        long accounts = accountRepository.countByCustomerId(customer.getId());
        long beneficiaries = beneficiaryRepository.countByCustomerId(customer.getId());
        long consents = consentRepository.countByCustomerId(customer.getId());
        if (accounts > 0) linked.add(accounts + (accounts == 1 ? " account" : " accounts"));
        if (beneficiaries > 0) linked.add(beneficiaries + (beneficiaries == 1 ? " beneficiary" : " beneficiaries"));
        if (consents > 0) linked.add(consents + (consents == 1 ? " consent" : " consents"));
        if (!linked.isEmpty()) {
            throw new IllegalStateException("Customer " + customer.getId() + " cannot be deleted because they still have "
                    + String.join(", ", linked) + ".");
        }
    }

    private CustomerResponse mapToResponse(Customer customer){
        return new CustomerResponse(
            customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone(), customer.getAddress()
        );
    }
}
