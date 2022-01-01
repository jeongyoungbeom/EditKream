package com.project.kream.Service;

import com.project.kream.Model.Entity.*;
import com.project.kream.Model.Header;
import com.project.kream.Model.Pagination;
import com.project.kream.Model.enumclass.CustomerRank;
import com.project.kream.Model.enumclass.CustomerRole;
import com.project.kream.Model.enumclass.CustomerType;
import com.project.kream.Model.request.CustomerApiRequest;
import com.project.kream.Model.response.*;
import com.project.kream.Repository.CustomerRepository;
import com.project.kream.Repository.PurchaseRepository;
import com.project.kream.Repository.SalesRepository;
import com.project.kream.Repository.Specification.CustomerSpecification;
import com.project.kream.Repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService extends BaseService<CustomerApiRequest, CustomerApiResponse, Customer> {
    private final CustomerRepository customerRepository;
    private final SalesRepository salesRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final CustomerSpecification customerSpecification;
    private final StyleCustomerService styleCustomerService;
    private final WithdrawalRepository withdrawalRepository;
    private final PurchaseRepository purchaseRepository;

    @Transactional
    public Header<Long> create(Header<CustomerApiRequest> request) {
        CustomerApiRequest customerApiRequest = request.getData();
        Customer customer1 = baseRepository.save(customerApiRequest.toEntity(passwordEncoder.encode(customerApiRequest.getUserpw())));

        String email = customer1.getEmail();

        styleCustomerService.create(customer1.getId(), email.substring(0,email.indexOf("@")));
        return Header.OK(customer1.getId());
    }

    public Header<CustomerApiResponse> read(Long id){
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 유저가 없음"));
        return Header.OK(new CustomerApiResponse(customer));
    }


    public Header delete(Long id){
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        Customer newCustomer = optionalCustomer.get();

        Withdrawal withdrawal = Withdrawal.builder()
                .email(newCustomer.getEmail())
                .hp(newCustomer.getHp())
                .build();
        withdrawalRepository.save(withdrawal);

        return optionalCustomer.map(customer -> {
            baseRepository.delete(customer);
            return Header.OK();
        }).orElseGet(() -> Header.ERROR("데이터 없음"));
    }

    public Long session(String email){
        Customer customer = customerRepository.getByEmail(email);
        return customer.getId();
    }

    public Boolean pwCheck(Long id, String userpw){
        Customer customer = baseRepository.getById(id);
        return passwordEncoder.matches(userpw, customer.getUserpw());
    }

//    public Header<CustomerApiResponse> update(Header<CustomerApiRequest> request) {
//        CustomerApiRequest customerApiRequest = request.getData();
//        Optional<Customer> customer = baseRepository.findById(customerApiRequest.getId());
//
//        return customer.map(user ->{
//                    user.setEmail(customerApiRequest.getEmail());
//                    user.setUserid(customerApiRequest.getUserid());
//                    if (customerApiRequest.getNewuserpw() != null){
//                        user.setUserpw(passwordEncoder.encode(customerApiRequest.getNewuserpw()));
//                    }else{
//                        user.setUserpw(customerApiRequest.getUserpw());
//                    }
//                    user.setHp(customerApiRequest.getHp());
//                    user.setShoesize(customerApiRequest.getShoesize());
//                    user.setAgreement(customerApiRequest.getAgreement());
//                    user.setPrivacyPolicy(customerApiRequest.getPrivacyPolicy());
//                    user.setSmsAllow(customerApiRequest.getSmsAllow());
//                    user.setEmailAllow(customerApiRequest.getEmailAllow());
//                    user.setMessage((customerApiRequest.getMessage()));
//                    user.setImage(customerApiRequest.getImage());
//                    user.setPoint(customerApiRequest.getPoint());
//                    user.setRank(customerApiRequest.getRank());
//                    user.setType(customerApiRequest.getType());
//                    return user;
//                }).map(custom -> baseRepository.save(custom))
//                .map(custom -> response(custom))
//                .map(Header::OK)
//                .orElseGet(() -> Header.ERROR("데이터가 없습니다."));
//    }

    @Transactional
    public Long update(Header<CustomerApiRequest> request) {
        CustomerApiRequest customerApiRequest = request.getData();
        Customer customer = customerRepository.findById(customerApiRequest.getId()).orElseThrow(() -> new IllegalArgumentException("해당유저 없음"));

        customer.update(customerApiRequest.getEmail(), customer.getUserid(), customer.getUserpw(), customerApiRequest.getHp(), customerApiRequest.getShoesize(), customerApiRequest.getAgreement(), customerApiRequest.getPrivacyPolicy(), customerApiRequest.getSmsAllow(), customerApiRequest.getEmailAllow(), customerApiRequest.getImage(), customerApiRequest.getPoint(), customerApiRequest.getMessage());
        return customerApiRequest.getId();
    }

    public Header<List<CustomerListApiResponse>> List(CustomerType type, Pageable pageable){
        Page<Customer> customerList = customerRepository.findAllByType(type, pageable);
        List<CustomerListApiResponse> customerListApiResponseList = customerList.stream()
                .map(customer -> new CustomerListApiResponse(customer))
                .collect(Collectors.toList());

        int countPage = 5;
        int startPage = (( customerList.getNumber()) / countPage) * countPage + 1;
        int endPage = startPage + countPage - 1;
        if(endPage >  customerList.getTotalPages()) {
            endPage =  customerList.getTotalPages();
        }

        Pagination pagination = Pagination.builder()
                .totalPages( customerList.getTotalPages())
                .totalElements( customerList.getTotalElements())
                .currentPage( customerList.getNumber())
                .currentElements( customerList.getNumberOfElements())
                .startPage(startPage)
                .endPage(endPage)
                .build();

        return Header.OK(customerListApiResponseList, pagination);
    }

    public void searchPW(String email, String hp){
        Customer customer = customerRepository.findByEmailAndHp(email, hp);
        mailService.execMail(customer.getEmail());
    }

    public StringBuffer searchEmail(String hp){
        Customer customer = customerRepository.findByHp(hp);
        StringBuffer sb = new StringBuffer();

        sb.append(customer.getEmail());

        int cnt = sb.substring(1, sb.indexOf("@")).length();

        sb.replace(1, sb.indexOf("@")-1, "*".repeat(cnt));
        return sb;
    }


    // 마이페이지
    public Header<CustomerMypage1ApiResponse> mypage(Long id){
        Customer customer = baseRepository.getById(id);

        List<Purchase> purchaseList = customer.getPurchaseList();
        List<CustomerMypagePurchaseApiResponse> customerMypagePurchaseApiResponseList = purchaseList.stream()
                .map(purchase -> {
                    Product product = purchase.getProduct();
                    return new CustomerMypagePurchaseApiResponse(product, purchase);
                }).collect(Collectors.toList());

        List<Sales> salesList = customer.getSalesList();
        List<CustomerMypageSalesApiResponse> customerMypageSalesApiResponseList = salesList.stream()
                .map(sales -> {
                    Product product = sales.getProduct();
                    return new CustomerMypageSalesApiResponse(product, sales);
                }).collect(Collectors.toList());

        List<Cart> cartList = customer.getCartList();
        List<CustomerMypageCartApiResponse> customerMypageCartApiResponseList = cartList.stream()
                .map(cart -> {
                    Product product = cart.getProduct();
                    return new CustomerMypageCartApiResponse(product, cart, salesRepository.findByProductId(product.getId()));
                }).collect(Collectors.toList());

        return Header.OK(new CustomerMypage1ApiResponse(customer, customerMypagePurchaseApiResponseList, customerMypageSalesApiResponseList, customerMypageCartApiResponseList));
    }

    // 구매내역
    public Header<List<CustomerPurchaseInfoApiResponse>> purchaseInfo(Long id, String regdate1, String regdate2){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd");
        LocalDateTime startDatetime = LocalDate.parse(regdate1, formatter).atStartOfDay();
        LocalDateTime endDatetime = LocalDate.parse(regdate2, formatter).atTime(23, 59, 59);

        List<Purchase> purchaseList = purchaseRepository.findAllByCustomerIdAndRegdate(id, startDatetime, endDatetime);

        List<CustomerPurchaseInfoApiResponse> customerPurchaseInfoApiResponseList = purchaseList.stream()
                .map(purchase -> {
                    Product product = purchase.getProduct();

                    CustomerPurchaseInfoApiResponse customerPurchaseInfoApiResponse = CustomerPurchaseInfoApiResponse.builder()
                            .id(purchase.getId())
                            .productId(product.getId())
                            .name(product.getName())
                            .originFileName(product.getProImgList().get(0).getOrigFileName())
                            .size(purchase.getSizeType())
                            .price(purchase.getPrice())
                            .period(purchase.getPeriod())
                            .status1(purchase.getStatus1())
                            .status2(purchase.getStatus2())
                            .status2(purchase.getStatus2())
                            .regdate(purchase.getRegdate())
                            .build();
                    return customerPurchaseInfoApiResponse;
                }).collect(Collectors.toList());

        return Header.OK(customerPurchaseInfoApiResponseList);
    }

    // 판매내역
    public Header<List<CustomerSalesInfoApiResponse>> salesInfo(Long id, String regdate1, String regdate2){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd");
        LocalDateTime startDatetime = LocalDate.parse(regdate1, formatter).atStartOfDay();
        LocalDateTime endDatetime = LocalDate.parse(regdate2, formatter).atTime(23, 59, 59);

        List<Sales> salesList = salesRepository.findAllByCustomerIdAndRegdate(id, startDatetime, endDatetime);
        List<CustomerSalesInfoApiResponse> customerSalesInfoApiResponseList = salesList.stream()
                .map(sales -> {
                    Product product = sales.getProduct();

                    CustomerSalesInfoApiResponse customerSalesInfoApiResponse = CustomerSalesInfoApiResponse.builder()
                            .id(sales.getId())
                            .productId(product.getId())
                            .name(product.getName())
                            .originFileName(product.getProImgList().get(0).getOrigFileName())
                            .size(sales.getSizeType())
                            .price(sales.getPrice())
                            .period(sales.getPeriod())
                            .status1(sales.getStatus1())
                            .status2(sales.getStatus2())
                            .status3(sales.getStatus3())
                            .regdate(sales.getRegdate())
                            .build();
                    return customerSalesInfoApiResponse;
                }).collect(Collectors.toList());

        return Header.OK(customerSalesInfoApiResponseList);
    }


    // 찜목록
    public Header<List<CustomerCartInfoApiResponse>> cartinfo(Long id){
        Customer customer = baseRepository.getById(id);

        List<Cart> cartList = customer.getCartList();
        List<CustomerCartInfoApiResponse> customerCartInfoApiResponseList = cartList.stream()
                .map(cart -> {
                    Product product = cart.getProduct();
                    CustomerCartInfoApiResponse customerCartInfoApiResponse = CustomerCartInfoApiResponse.builder()
                            .id(cart.getId())
                            .productId(product.getId())
                            .name(product.getName())
                            .brand(product.getBrand())
                            .originFileName(product.getProImgList().get(0).getOrigFileName())
                            .size(cart.getSizeType())
                            .price(salesRepository.findByProductId(product.getId()))
                            .build();
                    return customerCartInfoApiResponse;
                }).collect(Collectors.toList());

        return Header.OK(customerCartInfoApiResponseList);
    }


    // 관리자
    public Header<CustomerInfoApiResponse> customerInfo(Long id){
        Customer customer = baseRepository.getById(id);

        List<Address> addressList = customer.getAddressList();
        List<CustomerAddressApiResponse> customerAddressApiResponses = addressList.stream()
                .map(address -> {
                    CustomerAddressApiResponse customerAddressApiResponse = CustomerAddressApiResponse.builder()
                            .zipcode(address.getZipcode())
                            .address1(address.getDetail1())
                            .address2(address.getDetail2())
                            .build();
                    return customerAddressApiResponse;
                }).collect(Collectors.toList());

        List<CardInfo> cardInfoList = customer.getCardInfoList();
        List<CustomerCardInfoApiResponse> customerCardInfoApiResponses = cardInfoList.stream()
                .map(cardInfo -> {
                    CustomerCardInfoApiResponse customerCardInfoApiResponse = CustomerCardInfoApiResponse.builder()
                            .cardCompany(cardInfo.getCardCompany())
                            .cardNumber(cardInfo.getCardNumber())
                            .build();
                    return customerCardInfoApiResponse;
                }).collect(Collectors.toList());

        Account account = customer.getAccountList().get(0);
        String AccountNumber = "";
        String Bank = "";
        String Name = "";
        if (account==null) {
            AccountNumber = "정보없음";
            Bank = "정보없음";
            Name = "정보없음";
        }else{
            AccountNumber = account.getAccountNumber();
            Bank = account.getBank();
            Name = account.getName();
        }

        CustomerInfoApiResponse customerInfoApiResponse = CustomerInfoApiResponse.builder()
                .userid(customer.getEmail())
                .userpw(customer.getUserpw())
                .name(customer.getUserid())
                .hp(customer.getHp())
                .shoesize(customer.getShoesize())
                .rank(customer.getRank())
                .bank(Bank)
                .message(customer.getMessage())
                .agreement(customer.getAgreement())
                .accountNumber(AccountNumber)
                .accountname(Name)
                .privacyPolicy(customer.getPrivacyPolicy())
                .smsAllow(customer.getSmsAllow())
                .emailAllow(customer.getEmailAllow())
                .customerAddressApiResponseList(customerAddressApiResponses)
                .customerCardInfoApiResponseList(customerCardInfoApiResponses)
                .build();

        return Header.OK(customerInfoApiResponse);
    }

    public Header<List<CustomerSearchApiResponse>> dataList(Header<CustomerApiRequest> request, Pageable pageable){
        Page<Customer> customerList = customerSpecification.searchCustomerList(request, pageable);

        List<CustomerSearchApiResponse> customerSearchApiResponseList = customerList.stream()
                .map(customer -> {
                    CustomerSearchApiResponse customerSearchApiResponse = CustomerSearchApiResponse.builder()
                            .id(customer.getId())
                            .email(customer.getEmail())
                            .userid(customer.getUserid())
                            .rank(customer.getRank())
                            .message(customer.getMessage())
                            .regdate(customer.getRegdate())
                            .build();
                    return customerSearchApiResponse;
                }).collect(Collectors.toList());

        int countPage = 5;
        int startPage = ((customerList.getNumber()) / countPage) * countPage + 1;
        int endPage = startPage + countPage - 1;
        if(endPage > customerList.getTotalPages()) {
            endPage = customerList.getTotalPages();
        }

        Pagination pagination = Pagination.builder()
                .totalPages(customerList.getTotalPages())
                .totalElements(customerList.getTotalElements())
                .currentPage(customerList.getNumber())
                .currentElements(customerList.getNumberOfElements())
                .startPage(startPage)
                .endPage(endPage)
                .build();

        return Header.OK(customerSearchApiResponseList, pagination);
    }


    // 이미지 업로드
//    public Header<CustomerApiResponse> imageUpdate(Header<CustomerApiRequest> request) {
//        CustomerApiRequest customerApiRequest = request.getData();
//        Optional<Customer> customer = baseRepository.findById(customerApiRequest.getId());
//
//        return customer.map(user ->{
//                    user.setImage(customerApiRequest.getImage());
//                    return user;
//                }).map(custom -> baseRepository.save(custom))
//                .map(custom -> response(custom))
//                .map(Header::OK)
//                .orElseGet(() -> Header.ERROR("데이터가 없습니다."));
//    }

}
