package com.project.kream.Service;

import com.project.kream.Model.Entity.Board;
import com.project.kream.Model.Header;
import com.project.kream.Model.Pagination;
import com.project.kream.Model.enumclass.BoardCategory;
import com.project.kream.Model.request.BoardApiRequest;
import com.project.kream.Model.response.BoardApiResponse;
import com.project.kream.Model.response.BoardFaqApiResponse;
import com.project.kream.Model.response.BoardSearchApiResponse;
import com.project.kream.Repository.BoardRepository;
import com.project.kream.Repository.Specification.BoardSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService extends BaseService<BoardApiRequest, BoardApiResponse, Board> {
    private final BoardRepository boardRepository;
    private final BoardSpecification boardSpecification;

    public Long create(Header<BoardApiRequest> request) {
        BoardApiRequest boardApiRequest = request.getData();
        Board board = boardRepository.save(boardApiRequest.toEntity());
        return board.getId();
    }


    public Header<BoardApiResponse> read(Long id){
//        return baseRepository.findById(id)
//                .map(board -> response(board))
//                .map(Header::OK)
//                .orElseGet(
//                        () -> Header.ERROR("데이터없음")
//                );
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        return Header.OK(new BoardApiResponse(board));
    }

    public Long update(Header<BoardApiRequest> request) {
        BoardApiRequest boardApiRequest = request.getData();
        Board board = boardRepository.getById(boardApiRequest.getId());
        board.update(boardApiRequest.getTitle(), boardApiRequest.getContent(), boardApiRequest.getRegistrant(),boardApiRequest.getCategory());
        return board.getId();
    }

    public Header<List<BoardApiResponse>> List(Pageable pageable){
        Page<Board> boardList = baseRepository.findAll(pageable);
        List<BoardApiResponse> boardApiResponseList = boardList.stream()
                .map(BoardApiResponse::new)
                .collect(Collectors.toList());

        int countPage = 5;
        int startPage = ((boardList.getNumber()) / countPage) * countPage + 1;
        int endPage = startPage + countPage - 1;
        if(endPage > boardList.getTotalPages()) {
            endPage = boardList.getTotalPages();
        }

        Pagination pagination = Pagination.builder()
                .totalPages(boardList.getTotalPages())
                .totalElements(boardList.getTotalElements())
                .currentPage(boardList.getNumber())
                .currentElements(boardList.getNumberOfElements())
                .startPage(startPage)
                .endPage(endPage)
                .build();

        return Header.OK(boardApiResponseList, pagination);
    }
    public Header<List<BoardSearchApiResponse>> noticeList(Pageable pageable){
        Page<Board> boardList = boardRepository.notice(pageable);

        List<BoardSearchApiResponse> boardSearchApiResponseList = boardList.stream()
                .map(BoardSearchApiResponse::new).collect(Collectors.toList());

        int countPage = 5;
        int startPage = ((boardList.getNumber()) / countPage) * countPage + 1;
        int endPage = startPage + countPage - 1;
        if(endPage > boardList.getTotalPages()) {
            endPage = boardList.getTotalPages();
        }

        Pagination pagination = Pagination.builder()
                .totalPages(boardList.getTotalPages())
                .totalElements(boardList.getTotalElements())
                .currentPage(boardList.getNumber())
                .currentElements(boardList.getNumberOfElements())
                .startPage(startPage)
                .endPage(endPage)
                .build();

        return Header.OK(boardSearchApiResponseList, pagination);
    }

    public Header<List<BoardFaqApiResponse>> faqList(Pageable pageable){
        Page<Board> boardList = boardRepository.faq(pageable);

        List<BoardFaqApiResponse> BoardFaqApiResponseList = boardList.stream()
                .map(BoardFaqApiResponse::new).collect(Collectors.toList());

        int countPage = 5;
        int startPage = ((boardList.getNumber()) / countPage) * countPage + 1;
        int endPage = startPage + countPage - 1;
        if(endPage > boardList.getTotalPages()) {
            endPage = boardList.getTotalPages();
        }

        Pagination pagination = Pagination.builder()
                .totalPages(boardList.getTotalPages())
                .totalElements(boardList.getTotalElements())
                .currentPage(boardList.getNumber())
                .currentElements(boardList.getNumberOfElements())
                .startPage(startPage)
                .endPage(endPage)
                .build();

        return Header.OK(BoardFaqApiResponseList, pagination);
    }
    public Header<List<BoardSearchApiResponse>> dataList(Header<BoardApiRequest> request, Pageable pageable){
        Page<Board> boardList = boardSpecification.searchCustomerList(request, pageable);

        List<BoardSearchApiResponse> boardSearchApiResponseList = boardList.stream()
                .map(BoardSearchApiResponse::new).collect(Collectors.toList());

        int countPage = 5;
        int startPage = ((boardList.getNumber()) / countPage) * countPage + 1;
        int endPage = startPage + countPage - 1;
        if(endPage > boardList.getTotalPages()) {
            endPage = boardList.getTotalPages();
        }

        Pagination pagination = Pagination.builder()
                .totalPages(boardList.getTotalPages())
                .totalElements(boardList.getTotalElements())
                .currentPage(boardList.getNumber())
                .currentElements(boardList.getNumberOfElements())
                .startPage(startPage)
                .endPage(endPage)
                .build();
        return Header.OK(boardSearchApiResponseList, pagination);
    }

//    public Long delete(Long id){
////        Optional<Board> boardOptional = baseRepository.findById(id);
////        return boardOptional.map(board ->{
////            baseRepository.delete(board);
////            return Header.OK();
////        }).orElseGet(() -> Header.ERROR("데이터 없음"));
//        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 게시글 없음"));
//
//    }

    public int delete(Long id){
        Optional<Board> board = boardRepository.findById(id);
        if(board.isPresent()){
            boardRepository.delete(board.get());
            return 1;
        }
        return 0;
    }

//    public Header<List<BoardApiResponse>> paging(Pageable pageable){
//        Page<Board> board = baseRepository.findAll(pageable);
//        List<BoardApiResponse> boardApiResponseList = board.stream()
//                .map(users -> response(users))
//                .collect(Collectors.toList());
//        Pagination pagination = Pagination.builder()
//                .totalPages(board.getTotalPages())
//                .totalElements(board.getTotalElements())
//                .currentPage(board.getNumber())
//                .currentElements(board.getNumberOfElements())
//                .build();
//        return Header.OK(boardApiResponseList, pagination);
//    }

    public Header<List<BoardApiResponse>> categoryList(BoardCategory category){
        List<Board> boardList = boardRepository.findAllByCategory(category);
        List<BoardApiResponse> boardApiResponseList = boardList.stream()
                .map(BoardApiResponse::new)
                .collect(Collectors.toList());
        return Header.OK(boardApiResponseList);
    }


}
