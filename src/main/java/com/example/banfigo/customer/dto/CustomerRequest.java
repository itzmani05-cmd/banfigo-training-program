package com.example.banfigo.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class CustomerRequest {
    @NotBlank(message="Name is required")
    @Size(min=2, max=50, message="Name must be between 2 and 50 characters")
    private String name;

    @Email(message="Email is invalid")
    @NotBlank(message="Email is required")
    private String email;

    @Pattern(regexp="^\\+?[1-9]\\d{0,14}$", message="Phone number is invalid")
    @NotBlank(message="Phone number is required")
    private String phone;

    @NotBlank(message="Address is required")
    @Size(max=255, message="Address must be less than 255 characters")
    private String address;
    
    public CustomerRequest() {
    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phone){
        this.phone = phone;
    }
    public String getAddress(){
        return address;
    }
    public void setAddress(String address){
        this.address = address;
    }
}
