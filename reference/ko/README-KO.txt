[NOTICE]FOR Korean,
	이 번역물에서 예제 내의 주석에 대한 영문을 원문 그대로 두었습니다. 
	이유는 예제 부분에 한글 폰트를 적용할 경우에, 텍스트들이 일그러지는 문제가 
	존재하기에 기존의 번역된 예제 주석을 모두 롤백시켰습니다. 
	차후에 한글 폰트에 맞도록 형식을 재편집해야할 부분이니 양해바랍니다.
	그럼 하이버네이트와 즐거운 시간을 보내시길 바랍니다. 
	
	번역자 김종대(jdkim528@korea.com)
	Blog : http://blog.naver.com/jdkim528/

[Down/Config/Build]
	cvs 클라이언트를 갖고 있다면, 
	1)연결 유형	: pserve/extssh 중 하나를 선택합니다
	2)사용자이름	: anonymous
	3)호스트		: cvs.sourceforge.net
	4)포트		: 기본값
	5)저장소경로	: /cvsroot/hibernate
    6)모듈		: Hibernate3/doc/reference
	위와 같이 연결 정보를 입력 하신 후 REFERENCE 전체를 내려받으셔도 되지만,
	한글 번역본만 필요하시다면 다음과 같이 하셔도 됩니다.
	[가]. 공통 모듈 받기
	위에서 6)모듈을 Hibernate3/doc/reference/support로 지정하시고 
	로컬 컴퓨터의 디렉토리 [로컬 경로]/reference/ 하에 받습니다
	[나]. 한글본 모듈 받기
	위에서 6)모듈을 Hibernate3/doc/reference/ko로 지정하고 
	로컬 컴퓨터의 디렉토리 [로컬 경로]/reference/ 하에 받습니다.
	[다]. 빌드 파일 받기
	그런 다음 /doc/reference/build.xml 파일을 내려 받은 다음 
	아래 예와 같이 한글 번역본 외의 부분들을 주석처리합니다. 
	[라]. 빌드하기
	그런 다음  [로컬 경로]/reference/ 에서 ant all.doc로 빌드하시면 됩니다.
	빌드 시간은 2분 가량 소요됩니다.
	[마]. 문서 보기
	디렉토리 [로컬 경로]/reference/build/ko/ 디렉토리에 빌드된 문서를 보시기 바랍니다.
	그럼 하이버네이트와 함께 즐거운 시간을 보내세요.
	
	[예]
	    <target name="all.doc"
            depends="clean"
            description="Compile documentation for all languages and all formats.">

        <!-- TRANSLATOR: Duplicate this line for your language -->
        <!--antcall target="lang.all"><param name="lang" value="en"/></antcall-->
        <!--antcall target="lang.all"><param name="lang" value="zh-cn"/></antcall-->
        <!--antcall target="lang.all"><param name="lang" value="es"/></antcall-->
    	<antcall target="lang.all"><param name="lang" value="ko"/></antcall>

    </target>
    <target name="all.revdiff"
            description="Generates a diff report for all translated versions.">

        <!-- TRANSLATOR: Duplicate this line for your language -->
        <!--antcall target="lang.revdiff"><param name="lang" value="zh-cn"/></antcall-->
        <!--antcall target="lang.revdiff"><param name="lang" value="es"/></antcall-->
    	<antcall target="lang.revdiff"><param name="lang" value="ko"/></antcall>

    </target>
