package com.ssafy.pit.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.ssafy.pit.common.auth.PitUserDetails;
import com.ssafy.pit.common.response.BaseResponseBody;
import com.ssafy.pit.entity.User;
import com.ssafy.pit.request.ClassSearchGetReq;
import com.ssafy.pit.request.CreateClassPostReq;
import com.ssafy.pit.request.SetVideoUrlsPostReq;
import com.ssafy.pit.response.ClassDetailGetRes;
import com.ssafy.pit.response.ClassListGetRes;
import com.ssafy.pit.response.RegisterClassGetRes;
import com.ssafy.pit.service.ClassService;
import com.ssafy.pit.service.PtroomService;
import com.ssafy.pit.service.UserService;

@CrossOrigin(
        origins = {"https://i5a204.p.ssafy.io:5000", "https://i5a204.p.ssafy.io:8083", "https://localhost:8083"}, 
        allowCredentials = "true", 
        allowedHeaders = "*", 
        methods = {RequestMethod.GET,RequestMethod.POST,RequestMethod.DELETE,RequestMethod.PUT,RequestMethod.OPTIONS}
)
@RequestMapping("/v1/class")
@RestController
public class ClassController {
	
	@Autowired
	ClassService classService;
	
	@Autowired
	UserService userService;
	
	@Autowired
	PtroomService ptroomService;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	// 클래스 상세값 가져오기 (모든 사용자가 사용가능, 001만 조회)
	@GetMapping("/{classNo}")
	public ResponseEntity<ClassDetailGetRes> getClassDetail(@PathVariable int classNo){
		ClassDetailGetRes classDetail = classService.getClassDetail(classNo, "001");
		return ResponseEntity.status(200).body(classDetail);
	}
	
	// 클래스 리스트 가져오기
	@GetMapping()
	public ResponseEntity<List<ClassListGetRes>> getClassList(ClassSearchGetReq searchInfo) {
		List<ClassListGetRes> classList = null;
		classList = classService.getClassList("001");
		if(searchInfo.getSearchType() == null && searchInfo.getClassDay() == null && searchInfo.getClassLevel() == null
				&& searchInfo.getClassType() == null && searchInfo.getClassStartTime() == null 
				&& searchInfo.getClassEndTime() == null && searchInfo.getSearchKeyword() == null) {
			classList = classService.getClassList("001");
		}
		else {
			classList = classService.getClassList(searchInfo, "001");
		}
		return ResponseEntity.status(200).body(classList);
	}	
	
	// 관리자를 위한 클래스 리스트 가져오기 (authentication) permission(승인, 미승인, 거절)에 따른 클래스 목록보기 가능
	@GetMapping("/admin")
	public ResponseEntity<List<ClassListGetRes>> getAdminClassList(Authentication authentication, @RequestParam HashMap<String, String> permissionMap) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		if(userService.validateUserType(userEmail) == 1) {
			List<ClassListGetRes> classList = classService.getClassList(permissionMap.get("permission"));
			return ResponseEntity.status(200).body(classList);
		}
		else {			
			return ResponseEntity.status(403).body(null);
		}
	}
	
	// 관리자 클래스 상세 -> 승인, 미승인, 거절된 클래스 모두 상세보기 가능
	@GetMapping("/admin/{classNo}")
	public ResponseEntity<ClassDetailGetRes> getAdminClassDetail(Authentication authentication, @PathVariable int classNo) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		if(userService.validateUserType(userEmail) == 1) {
			ClassDetailGetRes classDetail = classService.getClassDetail(classNo, "000");
			return ResponseEntity.status(200).body(classDetail);
		}
		else {			
			return ResponseEntity.status(403).body(null);
		}		
	}
	
	
	// 찜클 목록 가져오기
	@GetMapping("/likes")
	public ResponseEntity<List<ClassListGetRes>> getClassLikesList(Authentication authentication) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		int userNo = userDetails.getUser().getUserNo();
		if(userService.validateUserType(userEmail) == 3) {
			List<ClassListGetRes> classLikesList = classService.getClassLikesList(userNo);
			return ResponseEntity.status(200).body(classLikesList);
		}
		else {
			return ResponseEntity.status(403).body(null);
		}
	}
	
	// 찜클 등록
	@PostMapping("/likes/{classNo}")
	public ResponseEntity<BaseResponseBody> registerClassLikes(Authentication authentication, @PathVariable int classNo){
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		int userNo = userDetails.getUser().getUserNo();
		if(userService.validateUserType(userEmail) == 3) {
			if(classService.registerClassLikes(userNo, classNo) == 1) {
				return ResponseEntity.status(200).body(BaseResponseBody.of(200, "찜한클래스로 등록되었습니다."));
			}
			else if(classService.registerClassLikes(userNo, classNo) == 2) {
				return ResponseEntity.status(200).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
			}
			else if(classService.registerClassLikes(userNo, classNo) == 3){				
				return ResponseEntity.status(409).body(BaseResponseBody.of(409, "이미 찜한 클래스입니다."));
			}
			else {
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "등록과정에 문제가 생겼습니다."));
				
			}
		}
		else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
		}
	}
	
	// 찜클 삭제
	@DeleteMapping("/likes/{classNo}")
	public ResponseEntity<BaseResponseBody> deleteClassLikes(Authentication authentication, @PathVariable int classNo){
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		int userNo = userDetails.getUser().getUserNo();
		if(userService.validateUserType(userEmail) == 3) {
			if(classService.deleteClassLikes(userNo, classNo) == 1) {
				return ResponseEntity.status(200).body(BaseResponseBody.of(200, "찜한클래스가 해제되었습니다."));
			}
			else {				
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "찜한 클래스 삭제과정에 문제가 생겼습니다."));
			}
		}
		else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
		}
	}
	
	// 수강완료 클래스
	@GetMapping("/finishedclass")
	public ResponseEntity<List<ClassListGetRes>> getFinishedClassList(Authentication authentication) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		int userNo = userDetails.getUser().getUserNo();
		if(userService.validateUserType(userEmail) == 3) {
			List<ClassListGetRes> finishedClassList = classService.getFinishedClassList(userNo);
			return ResponseEntity.status(200).body(finishedClassList);
		}
		else if(userService.validateUserType(userEmail) == 2) {
			List<ClassListGetRes> finishedClassList = classService.getFinishedTeachClassList(userNo);
			return ResponseEntity.status(200).body(finishedClassList);
		}
		else {
			return ResponseEntity.status(403).body(null);
		}
	}
	
	// 수강중 클래스
	@GetMapping("/registerclass")
	public ResponseEntity<List<RegisterClassGetRes>> getRegisterClassList(Authentication authentication) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		int userNo = userDetails.getUser().getUserNo();
		if(userService.validateUserType(userEmail) == 3) {
			List<RegisterClassGetRes> registerClassList = classService.getRegisterClassList(userNo);
			return ResponseEntity.status(200).body(registerClassList);
		}
		else if(userService.validateUserType(userEmail) == 2) {
			List<RegisterClassGetRes> registerClassList = classService.getTeachClassList(userNo);
			return ResponseEntity.status(200).body(registerClassList);
		}
		else {
			return ResponseEntity.status(403).body(null);
		}
	}
	
	// 클래스 생성 (트레이너가 관리자에게 승인신청)
	@PostMapping("/create")
	public ResponseEntity<BaseResponseBody> createClass(Authentication authentication, @ModelAttribute CreateClassPostReq createClassInfo, MultipartHttpServletRequest request) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		User user = userDetails.getUser();
		String userEmail = userDetails.getUsername();
		if(userService.validateUserType(userEmail) == 2) {
			try {
				// 클래스에 정보 추가
				classService.createClass(createClassInfo, user);
				// class_photo 테이블에 넣을 classNo 값 구하기
				int classNo = classService.getLatestClassNo();
				
				// 클래스 개설한 userName, classTitle, classDay를 encode 하여 ptroomSessionName 생성
				String ptroomSessionName = 
						passwordEncoder.encode(user.getUserName()+createClassInfo.getClassTitle()+createClassInfo.getClassDay());
				
				// ptroom url 생성
				ptroomService.createPtroom(ptroomSessionName, classNo);
				
				// 썸네일 이미지 넣기
				classService.createClassPhoto(request, classNo);
				// 서브이미지들 넣기
				classService.createSubPhotos(request, classNo);
				
				return ResponseEntity.status(200).body(BaseResponseBody.of(200, "해당 클래스가 승인요청 처리되었습니다."));				
			}
			catch (Exception e) {
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "클래스 승인요청 과정에 문제가 발생하였습니다."));
			}			
		}
		else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
		}
	}
	
	// 클래스 등록하기 (수강생이 클래스 신청하기)
	@PutMapping("/enrollment/{classNo}")
	public ResponseEntity<BaseResponseBody> enrollClass(Authentication authentication, @PathVariable int classNo) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		User user = userDetails.getUser();
		String userEmail = userDetails.getUsername();
		if(userService.validateUserType(userEmail) == 3) {
			try {
				if(classService.enrollClass(user, classNo) == 1) {
					return ResponseEntity.status(200).body(BaseResponseBody.of(200, "해당 클래스가 등록처리되었습니다."));	
				}
				else if (classService.enrollClass(user, classNo) == 2) {					
					return ResponseEntity.status(409).body(BaseResponseBody.of(409, "승인이 나지 않은 클래스입니다."));
				}
				else {
					return ResponseEntity.status(405).body(BaseResponseBody.of(405, "해당클래스의 정원이 초과하였습니다."));
				}
			}
			catch (Exception e) {
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "클래스 등록신청 과정에 문제가 발생하였습니다."));
			}
		}
		else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
		}
	}
	
	// 클래스 permission 변경 (관리자만 가능)
	@PutMapping("/admin/{classNo}")
	public ResponseEntity<BaseResponseBody> updateClassPermission(Authentication authentication, @RequestBody HashMap<String, String> permissionMap, @PathVariable int classNo) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		User user = userDetails.getUser();
		String userEmail = userDetails.getUsername();
		if(userService.validateUserType(userEmail) == 1) {
			try {
				String permission = permissionMap.get("permission");
				classService.updateClassPermission(classNo, permission);
				if(permission.equals("001")) {
					return ResponseEntity.status(200).body(BaseResponseBody.of(200, "해당클래스를 승인처리 하였습니다."));	
				} else if(permission.equals("002")) {
					return ResponseEntity.status(200).body(BaseResponseBody.of(200, "해당클래스를 미승인처리 하였습니다."));	
				} else if(permission.equals("003")) {
					return ResponseEntity.status(200).body(BaseResponseBody.of(200, "해당클래스를 거부처리 하였습니다."));	
				} else {
					return ResponseEntity.status(500).body(BaseResponseBody.of(500, "클래스 등록신청 과정에 문제가 발생하였습니다."));
				}
			} catch (Exception e) {
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "클래스 등록신청 과정에 문제가 발생하였습니다."));
			}
			
		}
		else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
		}
	}
	
	// 비디오 URL DB에 저장
	@PostMapping("/video/{classNo}")
	public ResponseEntity<BaseResponseBody> setVideoUrls(Authentication authentication, @PathVariable int classNo, @RequestBody SetVideoUrlsPostReq setVideoUrlsInfo){
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		User user = userDetails.getUser();
		String userEmail = userDetails.getUsername();
		int userNo = user.getUserNo();
		
		if(userService.validateUserType(userEmail) == 3) {
			try {
				classService.setVideoUrls(userNo, classNo, setVideoUrlsInfo);
				return ResponseEntity.status(200).body(BaseResponseBody.of(200, "DB에 URL이 저장되었습니다."));
			}
			catch (Exception e) {
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "DB 저장에 실패했습니다."));
			}
		} else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "알맞지 않은 사용자입니다."));
		}
	}
	
	// 영상 다시보기 비디오들 리스트
	@GetMapping("/video/{classNo}")
	public ResponseEntity<Map<String, List>> getVideoUrls(Authentication authentication, @PathVariable int classNo) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		User user = userDetails.getUser();
		String userEmail = userDetails.getUsername();
		int userNo = user.getUserNo();
		if(userService.validateUserType(userEmail) == 3) {
			try {				
				List<String> videoUrls = classService.getVideoUrls(userNo, classNo);
				List<Date> videoSaveTimes = classService.getSaveTimtes(userNo, classNo);
				Map<String, List> map = new HashMap<String, List>();
				map.put("videoUrls", videoUrls);
				map.put("videoSaveTimes", videoSaveTimes);
				return ResponseEntity.status(200).body(map);
			}
			catch (Exception e) {
				return ResponseEntity.status(500).body(null);
			}
			
		} else {
			return ResponseEntity.status(403).body(null);
		}
	}
	
	// 클래스 수업회수 추가하기
	@PutMapping("/cnt/{classNo}")
	public ResponseEntity<BaseResponseBody> updateClassCnt(Authentication authentication, @PathVariable int classNo) {
		PitUserDetails userDetails = (PitUserDetails) authentication.getDetails();
		String userEmail = userDetails.getUsername();
		if(userService.validateUserType(userEmail) == 2) {
			try {
				classService.addClassCnt(classNo);
				return ResponseEntity.status(200).body(BaseResponseBody.of(200, "클래스 수업횟수가 추가되었습니다."));
			}
			catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.status(500).body(BaseResponseBody.of(500, "수정에 오류가 생겼습니다."));
			}
		} else {
			return ResponseEntity.status(403).body(BaseResponseBody.of(403, "접근할 수 없는 페이지입니다."));
		}
	}
	
}
