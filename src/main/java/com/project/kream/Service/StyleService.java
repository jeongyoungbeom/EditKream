package com.project.kream.Service;

import com.project.kream.Model.Entity.*;
import com.project.kream.Model.Header;
import com.project.kream.Model.Pagination;
import com.project.kream.Model.request.StyleApiRequest;
import com.project.kream.Model.response.*;
import com.project.kream.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StyleService extends BaseService<StyleApiRequest, StyleApiResponse, Style> {
    private final StyleRepository styleRepository;
    private final StyleReplyRepository styleReplyRepository;
    private final ProductTagService productTagService;
    private final HashTagService hashTagService;
    private final StyleimgService styleimgService;
    private final StyleimgRepository styleImgRepository;
    private final CustomerRepository customerRepository;
    private final SalesRepository salesRepository;
    private final ProductTagRepository productTagRepository;
    private final StyleHashTagRepository styleHashTagRepository;
    private final FollowRepository followRepository;
    private final HashTagRepository hashTagRepository;
    private final StyleHashTagService styleHashTagService;
    private final StyleLikeService styleLikeService;
    private final ReplyLikeService replyLikeService;
    private final FollowService followService;

    public Header<StyleApiResponse> create(StyleApiRequest request, MultipartHttpServletRequest multipart) {
        Style style = Style.builder()
                .customer(customerRepository.getById(request.getCustomerId()))
                .content(request.getContent())
                .hit(0L)
                .build();
        Style newStyle = baseRepository.save(style);


        List<MultipartFile> fileList = multipart.getFiles("files");
        String path = "/Users/soyounjeong/EditKream/src/main/resources/static/lib/styleImg";

        for(MultipartFile mf : fileList){
            String originFileName = mf.getOriginalFilename();
            long fileSize = mf.getSize();

            String safeFile = path + originFileName;

            try{
                mf.transferTo(new File(safeFile));
                styleimgService.create(originFileName, fileSize,  safeFile,  newStyle.getId());
            }catch (IllegalStateException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        for(Long id : request.getProductId()){
            productTagService.create(id, newStyle.getId());
        }

        for(String name : request.getTagName()){
            if(hashTagRepository.existsByTagName(name)){
                HashTag HashTag = hashTagRepository.getByTagName(name);
                styleHashTagService.create(newStyle.getId(), HashTag.getId());
            }else{
                Long hashTagId = hashTagService.create(name);
                styleHashTagService.create(newStyle.getId(), hashTagId);
            }
        }
        return Header.OK(response(newStyle));
    }

    public Header<StyleApiResponse> update(StyleApiRequest request) {
        Optional<Style> optionalStyle = styleRepository.findById(request.getId());
        return optionalStyle.map(style -> {
                    style.setContent(request.getContent());

                    productTagRepository.deleteAllByStyleId(request.getId());
                    styleHashTagRepository.deleteAllByStyleId(request.getId());

                    if(request.getProductId().length != 0) {
                        for (Long id : request.getProductId()) {
                            productTagService.create(id, style.getId());
                        }
                    }

                    if(request.getTagName().length != 0) {
                        for (String Name : request.getTagName()) {
                            if(hashTagRepository.existsByTagName(Name)){
                                HashTag HashTag = hashTagRepository.getByTagName(Name);
                                styleHashTagService.create(request.getId(), HashTag.getId());
                            }else{
                                Long hashTagId = hashTagService.create(Name);
                                styleHashTagService.create(request.getId(), hashTagId);
                            }
                        }
                    }
                    return style;
                }).map(style -> baseRepository.save(style))
                .map(this::response)
                .map(Header::OK)
                .orElseGet(() -> Header.ERROR("데이터가 없습니다"));
    }

    public Header<StyleApiResponse> upload(Long id, MultipartHttpServletRequest multiRequest){
        List<MultipartFile> fileList = multiRequest.getFiles("files");
        String path = "/Users/soyounjeong/EditKream/src/main/resources/static/lib/styleImg";

        List<StyleImg> styleImgList = styleImgRepository.findAllByStyleId(id);
        styleImgList.stream().map(styleImg ->{
            File file = new File(styleImg.getFilePath());
            if(file.exists()){
                file.delete();
            }
            styleImgRepository.delete(styleImg);
            return Header.OK();
        }).collect(Collectors.toList());

        for(MultipartFile mf : fileList){
            String originFileName = mf.getOriginalFilename();
            long fileSize = mf.getSize();
            String safeFile = path + originFileName;

            try{
                mf.transferTo(new File(safeFile));
                styleimgService.create(originFileName, fileSize,  safeFile,  id);
            }catch (IllegalStateException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return Header.OK();
    }

    // 히트 순서
    public Header<List<StyleHitListApiResponse>> styleHitList(Long sessionId) {
        List<Style> styleList = baseRepository.findAll();
        List<String> stringList = hashTagRepository.HashTagNameTop5();
        List<StyleHitListApiResponse> styleListApiResponseHitList = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = style.getStyleHashTagList().stream()
                            .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

                    List<StyleProductTagApiResponse> styleProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleProductTagApiResponse(productTag.getProduct(), productTag, salesRepository.findByProductId(productTag.getProduct().getId()))).collect(Collectors.toList());

                    return new StyleHitListApiResponse(style, styleCustomer, customer, styleImgRepository.countByStyleId(style.getId()), styleProductTagApiResponseList, styleHashTagNameApiResponseList,styleReplyRepository.countByStyleId(style.getId()), styleLikeService.liked(sessionId, style.getId()));
                }).sorted(Comparator.comparingLong(StyleHitListApiResponse::getHit).reversed())
                .collect(Collectors.toList());

        return Header.OK(styleListApiResponseHitList, stringList);
    }

    public Header<List<StyleHitListApiResponse>> noStyleHitList() {
        List<Style> styleList = baseRepository.findAll();
        List<String> stringList = hashTagRepository.HashTagNameTop5();

        List<StyleHitListApiResponse> styleListApiResponseHitList = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = style.getStyleHashTagList().stream()
                            .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

                    List<StyleProductTagApiResponse> styleProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleProductTagApiResponse(productTag.getProduct(), productTag,salesRepository.findByProductId(productTag.getProduct().getId()) )).collect(Collectors.toList());

                    return new StyleHitListApiResponse(style,styleCustomer,customer,styleImgRepository.countByStyleId(style.getId()),styleProductTagApiResponseList, styleHashTagNameApiResponseList, styleReplyRepository.countByStyleId(style.getId()), false);
                }).sorted(Comparator.comparingLong(StyleHitListApiResponse::getHit).reversed())
                .collect(Collectors.toList());

        return Header.OK(styleListApiResponseHitList,stringList);
    }

    // 유저정보
    public Header<StyleUserInfoApiResponse> styleUserList(Long customerId, Long sessionId) {
        Customer customer = customerRepository.getById(customerId);
        List<Style> styleList = customer.getStyleList();

        List<StyleUserListApiResponse> styleUserListApiResponseList = styleList.stream()
                .map(style -> {
                    List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = style.getStyleHashTagList().stream()
                            .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

                    List<StyleProductTagApiResponse> styleProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleProductTagApiResponse(productTag.getProduct(), productTag,salesRepository.findByProductId(productTag.getProduct().getId()) )).collect(Collectors.toList());

                    return new StyleUserListApiResponse(style, styleProductTagApiResponseList, styleHashTagNameApiResponseList,styleReplyRepository.countByStyleId(style.getId()),styleImgRepository.countByStyleId(style.getId()),styleLikeService.liked(sessionId, style.getId()));

                }).sorted(Comparator.comparing(StyleUserListApiResponse::getRegdate).reversed())
                .collect(Collectors.toList());

        List<Follow> followerList = customer.getFollowerList();
        List<FollowingListApiResponse> followingListApiResponseList = followerList.stream()
                .map(follow -> new FollowingListApiResponse(follow,followRepository.existsByFollowingIdAndFollowerId(follow.getFollowing().getId(), sessionId))).collect(Collectors.toList());

        List<Follow> followingList = customer.getFollowingList();
        List<FollowerListApiResponse> followerListApiResponseList = followingList.stream()
                .map(follow -> new FollowerListApiResponse(follow, followRepository.existsByFollowingIdAndFollowerId(follow.getFollower().getId(), sessionId))).collect(Collectors.toList());

        String intro = customer.getStyleCustomerList().get(0).getIntro();
        if(intro == null){
            intro = "정보없음";
        }
        return Header.OK(new StyleUserInfoApiResponse(customer, intro, styleRepository.countByCustomerId(customerId), followingListApiResponseList, followRepository.countByFollowingId(customerId), followerListApiResponseList, followRepository.countByFollowerId(customerId), followService.linked(customerId,sessionId), styleUserListApiResponseList));
    }

    public Header<StyleUserInfoApiResponse> noStyleUserList(Long customerId) {
        Customer customer = customerRepository.getById(customerId);
        List<Style> styleList = customer.getStyleList();

        List<StyleUserListApiResponse> styleUserListApiResponseList = styleList.stream()
                .map(style -> {
                    List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = style.getStyleHashTagList().stream()
                            .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

                    List<StyleProductTagApiResponse> styleProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleProductTagApiResponse(productTag.getProduct(), productTag, salesRepository.findByProductId(productTag.getProduct().getId()))).collect(Collectors.toList());
                    return new StyleUserListApiResponse(style, styleProductTagApiResponseList, styleHashTagNameApiResponseList,styleReplyRepository.countByStyleId(style.getId()), styleImgRepository.countByStyleId(style.getId()), false);

                }).sorted(Comparator.comparing(StyleUserListApiResponse::getRegdate).reversed())
                .collect(Collectors.toList());

        List<Follow> followerList = customer.getFollowerList();
        List<FollowingListApiResponse> followingListApiResponseList = followerList.stream()
                .map(follow -> new FollowingListApiResponse(follow, false)).collect(Collectors.toList());

        List<Follow> followingList = customer.getFollowingList();
        List<FollowerListApiResponse> followerListApiResponseList = followingList.stream()
                .map(follow -> new FollowerListApiResponse(follow, false)).collect(Collectors.toList());

        String intro = customer.getStyleCustomerList().get(0).getIntro();
        if(intro == null){
            intro = "정보없음";
        }

        return Header.OK(new StyleUserInfoApiResponse(customer, intro,styleRepository.countByCustomerId(customerId), followingListApiResponseList, followRepository.countByFollowingId(customerId), followerListApiResponseList, followRepository.countByFollowerId(customerId), false, styleUserListApiResponseList ));
    }

    public Header<List<StyleHashListApiResponse>> styleHashList(String tagName, Long sessionId) {
        List<Style> styleList = styleRepository.findAllBy(tagName);
        List<StyleHashListApiResponse> styleHashListApiResponseList = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = style.getStyleHashTagList().stream()
                            .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

                    List<StyleProductTagApiResponse> styleProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleProductTagApiResponse(productTag.getProduct(), productTag, salesRepository.findByProductId(productTag.getProduct().getId()))).collect(Collectors.toList());

                    return new StyleHashListApiResponse(styleCustomer, style, customer,styleImgRepository.countByStyleId(style.getId()), styleProductTagApiResponseList, styleHashTagNameApiResponseList, styleReplyRepository.countByStyleId(style.getId()), styleLikeService.liked(sessionId, style.getId()));

                }).sorted(Comparator.comparing(StyleHashListApiResponse::getRegdate).reversed())
                .collect(Collectors.toList());
        return Header.OK(styleHashListApiResponseList);
    }

    public Header<List<StyleHashListApiResponse>> noStyleHashList(String tagName) {
        List<Style> styleList = styleRepository.findAllBy(tagName);
        List<StyleHashListApiResponse> styleHashListApiResponseList = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = style.getStyleHashTagList().stream()
                            .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

                    List<StyleProductTagApiResponse> styleProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleProductTagApiResponse(productTag.getProduct(), productTag, salesRepository.findByProductId(productTag.getProduct().getId()))).collect(Collectors.toList());

                    return new StyleHashListApiResponse(styleCustomer, style, customer,styleImgRepository.countByStyleId(style.getId()), styleProductTagApiResponseList, styleHashTagNameApiResponseList, styleReplyRepository.countByStyleId(style.getId()), false );

                }).sorted(Comparator.comparing(StyleHashListApiResponse::getRegdate).reversed())
                .collect(Collectors.toList());
        return Header.OK(styleHashListApiResponseList);
    }

    public Header<List<StyleDetailListApiResponse>> detailList(Long sessionId){
        List<Style> styleList = baseRepository.findAll();
        List<StyleDetailListApiResponse> styleDetailListApiResponses = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleImgListApiResponse> styleImgListApiResponseList = style.getStyleImgList().stream()
                            .map(StyleImgListApiResponse::new).collect(Collectors.toList());

                    List<StyleDetailProductTagApiResponse> styleDetailProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleDetailProductTagApiResponse(productTag.getProduct(),salesRepository.findByProductId(productTag.getProduct().getId()) )).collect(Collectors.toList());

                    List<StyleDetailHashTagApiResponse> styleDetailHashTagApiResponseList = style.getStyleHashTagList().stream()
                            .map(hashTag -> new StyleDetailHashTagApiResponse(hashTag.getHashTag().getTagName())).collect(Collectors.toList());

                    List<StyleReply> styleReplyList = styleReplyRepository.ReplyList(style.getId());
                    List<StyleReplyDetailApiResponse> styleReplyDetailApiResponseList = styleReplyList.stream()
                            .map(styleReply -> new StyleReplyDetailApiResponse(styleReply, replyLikeService.liked(sessionId, styleReply.getId()))).collect(Collectors.toList());

                    return new StyleDetailListApiResponse(style, styleImgListApiResponseList, styleDetailProductTagApiResponseList, styleDetailHashTagApiResponseList, styleReplyDetailApiResponseList,styleLikeService.liked(sessionId, style.getId()),followService.linked(customer.getId(),sessionId));

                }).collect(Collectors.toList());
        return Header.OK(styleDetailListApiResponses);
    }
    // 여기까지 햇음

    public Header<List<StyleDetailListApiResponse>> noDetailList(){
        List<Style> styleList = baseRepository.findAll();
        List<StyleDetailListApiResponse> styleDetailListApiResponses = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleImgListApiResponse> styleImgListApiResponseList = style.getStyleImgList().stream()
                            .map(StyleImgListApiResponse::new).collect(Collectors.toList());

                    List<StyleDetailProductTagApiResponse> styleDetailProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleDetailProductTagApiResponse(productTag.getProduct(), salesRepository.findByProductId(productTag.getProduct().getId()) )).collect(Collectors.toList());

                    List<StyleDetailHashTagApiResponse> styleDetailHashTagApiResponseList = style.getStyleHashTagList().stream()
                            .map(hashTag -> new StyleDetailHashTagApiResponse(hashTag.getHashTag().getTagName())).collect(Collectors.toList());

                    List<StyleReply> styleReplyList = styleReplyRepository.ReplyList(style.getId());
                    List<StyleReplyDetailApiResponse> styleReplyDetailApiResponseList = styleReplyList.stream()
                            .map(styleReply -> new StyleReplyDetailApiResponse(styleReply, false)).collect(Collectors.toList());

                    StyleDetailListApiResponse styleDetailListApiResponse = StyleDetailListApiResponse.builder()
                            .styleId(style.getId())
                            .customerId(customer.getId())
                            .customerUserId(styleCustomer.getProfileName())
                            .customerOriginFile(customer.getImage())
                            .hit(style.getHit())
                            .content(style.getContent())
                            .styleImgListApiResponseList(styleImgListApiResponseList)
                            .styleDetailProductTagApiResponseList(styleDetailProductTagApiResponseList)
                            .styleDetailHashTagApiResponseList(styleDetailHashTagApiResponseList)
                            .styleReplyDetailApiResponseList(styleReplyDetailApiResponseList)
                            .hitBoolean(false)
                            .followBoolean(false)
                            .regdate(style.getRegdate())
                            .build();
                    return new StyleDetailListApiResponse(style, styleImgListApiResponseList, styleDetailProductTagApiResponseList, styleDetailHashTagApiResponseList, styleReplyDetailApiResponseList, false, false);

                }).collect(Collectors.toList());
        return Header.OK(styleDetailListApiResponses);
    }


    public Header<List<StyleDetailListApiResponse>> detailFollowingList(Long followerId){
        List<Style> styleList = styleRepository.findAllByFollowerId(followerId);
        List<StyleDetailListApiResponse> styleDetailListApiResponses = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleImgListApiResponse> styleImgListApiResponseList = style.getStyleImgList().stream()
                            .map(StyleImgListApiResponse::new).collect(Collectors.toList());

                    List<StyleDetailProductTagApiResponse> styleDetailProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleDetailProductTagApiResponse(productTag.getProduct(),salesRepository.findByProductId(productTag.getProduct().getId()) )).collect(Collectors.toList());

                    List<StyleDetailHashTagApiResponse> styleDetailHashTagApiResponseList = style.getStyleHashTagList().stream()
                            .map(hashTag -> new StyleDetailHashTagApiResponse(hashTag.getHashTag().getTagName())).collect(Collectors.toList());

                    List<StyleReply> styleReplyList = styleReplyRepository.ReplyList(style.getId());
                    List<StyleReplyDetailApiResponse> styleReplyDetailApiResponseList = styleReplyList.stream()
                            .map(styleReply -> new StyleReplyDetailApiResponse(styleReply, replyLikeService.liked(followerId, styleReply.getId()))).collect(Collectors.toList());

                    return new StyleDetailListApiResponse(style, styleImgListApiResponseList, styleDetailProductTagApiResponseList, styleDetailHashTagApiResponseList, styleReplyDetailApiResponseList,styleLikeService.liked(followerId, style.getId()), followService.linked(followerId, customer.getId()) );

                }).collect(Collectors.toList());
        return Header.OK(styleDetailListApiResponses);
    }



    public Header<List<StyleDetailListApiResponse>> detailUserList(Long customerId, Long sessionId){
        List<Style> styleList = styleRepository.findAllByCustomerId(customerId);
        List<StyleDetailListApiResponse> styleDetailListApiResponses = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleImgListApiResponse> styleImgListApiResponseList = style.getStyleImgList().stream()
                            .map(StyleImgListApiResponse::new).collect(Collectors.toList());

                    List<StyleDetailProductTagApiResponse> styleDetailProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleDetailProductTagApiResponse(productTag.getProduct(),salesRepository.findByProductId(productTag.getProduct().getId()) ) ).collect(Collectors.toList());

                    List<StyleDetailHashTagApiResponse> styleDetailHashTagApiResponseList = style.getStyleHashTagList().stream()
                            .map(hashTag -> new StyleDetailHashTagApiResponse(hashTag.getHashTag().getTagName())).collect(Collectors.toList());

                    List<StyleReply> styleReplyList = styleReplyRepository.ReplyList(style.getId());
                    List<StyleReplyDetailApiResponse> styleReplyDetailApiResponseList = styleReplyList.stream()
                            .map(styleReply -> new StyleReplyDetailApiResponse(styleReply,replyLikeService.liked(sessionId, styleReply.getId())) ).collect(Collectors.toList());

                    return new StyleDetailListApiResponse(style, styleImgListApiResponseList, styleDetailProductTagApiResponseList, styleDetailHashTagApiResponseList, styleReplyDetailApiResponseList,styleLikeService.liked(sessionId, style.getId()),followService.linked(sessionId, customer.getId()));

                }).collect(Collectors.toList());
        return Header.OK(styleDetailListApiResponses);
    }

    public Header<List<StyleDetailListApiResponse>> noDetailUserList(Long customerId){
        List<Style> styleList = styleRepository.findAllByCustomerId(customerId);
        List<StyleDetailListApiResponse> styleDetailListApiResponses = styleList.stream()
                .map(style -> {
                    Customer customer = style.getCustomer();
                    StyleCustomer styleCustomer = customer.getStyleCustomerList().get(0);
                    List<StyleImgListApiResponse> styleImgListApiResponseList = style.getStyleImgList().stream()
                            .map(StyleImgListApiResponse::new).collect(Collectors.toList());

                    List<StyleDetailProductTagApiResponse> styleDetailProductTagApiResponseList = style.getProductTagList().stream()
                            .map(productTag -> new StyleDetailProductTagApiResponse(productTag.getProduct(), salesRepository.findByProductId(productTag.getProduct().getId()))).collect(Collectors.toList());

                    List<StyleDetailHashTagApiResponse> styleDetailHashTagApiResponseList = style.getStyleHashTagList().stream()
                            .map(hashTag -> new StyleDetailHashTagApiResponse(hashTag.getHashTag().getTagName())).collect(Collectors.toList());

                    List<StyleReply> styleReplyList = styleReplyRepository.ReplyList(style.getId());
                    List<StyleReplyDetailApiResponse> styleReplyDetailApiResponseList = styleReplyList.stream()
                            .map(styleReply -> new StyleReplyDetailApiResponse(styleReply, false)).collect(Collectors.toList());

                    return new StyleDetailListApiResponse(style, styleImgListApiResponseList, styleDetailProductTagApiResponseList, styleDetailHashTagApiResponseList, styleReplyDetailApiResponseList, false, false);

                }).collect(Collectors.toList());
        return Header.OK(styleDetailListApiResponses);
    }


    public int delete(Long id){
        Optional<Style> optionalStyle = baseRepository.findById(id);
        if(optionalStyle.isPresent()){
            styleRepository.delete(optionalStyle.get());
            return  1;
        }
        return 0;
    }

    public StyleApiResponse response(Style style){
        StyleApiResponse styleApiResponse = StyleApiResponse.builder()
                .id(style.getId())
                .content(style.getContent())
                .customerId(style.getCustomer().getId())
                .build();
        return styleApiResponse;
    }

    public Header<List<StyleApiResponse>> search(Pageable pageable){
        Page<Style> styles = baseRepository.findAll(pageable);
        List<StyleApiResponse> styleApiResponseList = styles.stream()
                .map(StyleApiResponse::new)
                .collect(Collectors.toList());
        Pagination pagination = Pagination.builder()
                .totalPages(styles.getTotalPages())
                .totalElements(styles.getTotalElements())
                .currentPage(styles.getNumber())
                .build();

        return Header.OK(styleApiResponseList, pagination);
    }

    // 스타일 detail
    public Header<StyleDetailApiResponse> detail(Long styleId){
        Style style = baseRepository.getById(styleId);

        List<StyleImg> styleImgList = style.getStyleImgList();
        List<StyleImgListApiResponse> styleImgListApiResponseList = styleImgList.stream()
                .map(StyleImgListApiResponse::new).collect(Collectors.toList());

        List<ProductTag> productTagList = style.getProductTagList();
        List<StyleProductTagIdApiResponse> styleProductTagIdApiResponseList = productTagList.stream()
                .map(StyleProductTagIdApiResponse::new).collect(Collectors.toList());

        List<StyleHashTag> hashTagList = style.getStyleHashTagList();
        List<StyleHashTagNameApiResponse> styleHashTagNameApiResponseList = hashTagList.stream()
                .map(StyleHashTagNameApiResponse::new).collect(Collectors.toList());

        return Header.OK(new StyleDetailApiResponse(style, styleImgListApiResponseList, styleProductTagIdApiResponseList, styleHashTagNameApiResponseList));
    }
}