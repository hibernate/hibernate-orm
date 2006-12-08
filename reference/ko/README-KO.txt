[NOTICE]FOR Korean,
	이 번역물에서 예제 내의 주석에 대한 영문을 원문 그대로 두었습니다. 
	이유는 예제 부분에 한글 폰트를 적용할 경우에, 텍스트들이 일그러지는 문제가 
	존재하기에 기존의 번역된 예제 주석을 모두 롤백시켰습니다. 
	차후에 한글 폰트에 맞도록 형식을 재편집해야할 부분이니 양해바랍니다.
	그럼 하이버네이트와 즐거운 시간을 보내시길 바랍니다. 
	
	번역자 김종대(jdkim528@korea.com)
	Blog : http://blog.naver.com/jdkim528/

[Down/Config/Build]
 * 기존의 CVS에서 SVN으로 바뀌면서 신규 사용자들을 위한 길잡이를 작성할 필요가 
     생겼네요... 
  
    필자는 개인적으로 TortoiseSVN과 Subclipse를 사용하고 있으나 Subclpse를 중심으로 
    설명할까 합니다.(선호하는 svn 클라이언트가 있다면 해당 클라이언트의 설정에 
    따라 설정한 후에 사용하셔도 될 것입니다.)
        
        [Subclipse 설치]
        
        Subclipse를 설치하는 방법은 http://subclipse.tigris.org/install.html 을 참조하여 
        eclipse의 도움말>소프트웨어갱신>찾기 및 설치 메뉴를 통해 쉽게 설치할 수 있습니다.
        
        [Subclipse Checkout]
        0) Subclipse 설치가 끝났으면 이클립스를 재시작하십시오.
        1) 이클립스의 Perspective 열기에서 SVN Repository exploring을 열어 저장소를 등록하여 사용하거나
           이클립스의 패키지 탐색기에서 마우스 오른쪽 팝업 메뉴에서 "프로젝트"를 생성시킵니다.
           여기서는 "프로젝트" 생성 방법을 설명합니다.
        2) 이클립스의 패키지 탐색기에서 마우스 오른쪽 팝업 메뉴에서 "프로젝트"를 클릭한다
        3) 팝업 창의 "SVN" 노드에서 "Checkout Projects from SVN"을 선택한다
        4) "다음" 버튼을 클릭한다
        5) "Create a new respository location"을 선택한다
        6) 다음" 버튼을 클릭한다
        7) Location url에 "http://anonhibernate.labs.jboss.com/trunk/Hibernate3" 또는 
           "https://hibernate.labs.jboss.com/repos/hibernate/trunk" 을 입력합니다.
        8) "Hibernate3" 노드는 선택하거나 하위의 특정 노드를 선택하고 "완료" 버튼을 클릭한다.
        9) 프로젝트 명을 hibernate3 등의 원하는 이름으로 명명한다.
        10) checkout이 실행됩니다.
         
        [TortoiseSVN 설치]
	TortoiseSVN 클라이언트를 설치하셨다면, 시스템을 재시작 하십시오.
	1)레파지토리를 위한 폴더를 하나 생성시킵니다.(D:\repo)
	2)윈도우탐색기에서 D:\repo를 마우스 오른쪽 클릭한 후  TortoiseSVN 메뉴에서 
	  "Create repository Here..."를 클릭하면 팝업이 뜨는데, 
	   파일시스템/버클리DB 형식 중 하나를  선택하고 OK 버튼을 누르세요.
	3)hibernate를 내려 받기 위한 폴더를 하나 생성시키세요(D:\repo\Hibernate3)
	4)D:\repo\hibernate 폴더를 마우스 오른쪽 클릭한 후, 
	  TortoiseSVN 팝업 메뉴에서 CheckOut을 클릭하십시오.
	5)URL repository에 "http://anonhibernate.labs.jboss.com/trunk/Hibernate3" 또는 
           "https://hibernate.labs.jboss.com/repos/hibernate/trunk" 를 입력하고, 
	  OK 버튼을 클릭하십시오
	6)모두 내려받으신 다음에 D:\repo\Hibernate3\doc\reference로 이동합니다.
	7)이제 빌드하시면 됩니다.
	
	*) 한글 번역본만 필요하시다면 다음과 같이 하셔도 됩니다.
	/doc/reference/build.xml 파일을 
	아래 예와 같이 한글 번역본 외의 부분들을 주석처리합니다. 
	[가]. 빌드하기
	그런 다음  [로컬 경로]/reference/ 에서 ant all.doc로 빌드하시면 됩니다.
	빌드 시간은 2분 가량 소요됩니다.
	[나]. 문서 보기
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
