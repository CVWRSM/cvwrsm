!     Last change:  ED   14 Dec 2006   10:13 am
!     Copyright (C) 1998, 2000 State of California, Department of Water
!     Resources.

!     This program is licensed to you under the terms of the GNU General
!     Public License, version 2, as published by the Free Software
!     Foundation.

!     You should have received a copy of the GNU General Public License
!     along with this program; if not, contact Dr. Sushil Arora, below,
!     or the Free Software Foundation, 675 Mass Ave, Cambridge, MA
!     02139, USA.

!     THIS SOFTWARE AND DOCUMENTATION ARE PROVIDED BY THE CALIFORNIA
!     DEPARTMENT OF WATER RESOURCES AND CONTRIBUTORS "AS IS" AND ANY
!     EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
!     IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
!     PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE CALIFORNIA
!     DEPARTMENT OF WATER RESOURCES OR ITS CONTRIBUTORS BE LIABLE FOR
!     ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
!     CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
!     OR SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA OR PROFITS; OR
!     BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
!     LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
!     (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
!     USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
!     DAMAGE.

!     For more information, contact:

!     Dr. Sushil Arora
!     California Dept. of Water Resources
!     Division of Planning, Delta Modeling Section
!     1416 Ninth Street
!     Sacramento, CA  95814
!     916-653-7921
!     sushil@water.ca.gov



!   Written by:  Armin Munevar, California Department of Water Resources

!   This program is the main executive for the current Prototype.  It calls the
!   subroutine CODE generated by the interpreter mkcode.exe and the subroutine xamain
!   for the solution. The file 'rcc.out' is made as an intermediate step and is available
!   upon output.

SUBROUTINE WRAPPER (date, code, dss_init, reportsv, runDirectory)

  USE RCC_CACHE
  USE xasolver
  USE wrapper_utils
  USE wrangler
  USE Messenger

  IMPLICIT NONE
  include 'dwmy_type_definition.inc'
  DLL_EXPORT wrapper

  ! Dummy argument declarations
  EXTERNAL code, dss_init, reportsv
  TYPE(dwmy), INTENT(INOUT)       :: date

  !CHARACTER(LEN=32), parameter     :: version_identifier = '1.3.4 Beta (XA16)' 
  include '..\..\version_wrapper.inc'
  CHARACTER(LEN=75), parameter    :: copyright = 'This program is Copyright (C) 1998, 2010 State of California, all rights reserved'
  TYPE(rcc),DIMENSION(:), ALLOCATABLE :: problem
  TYPE(answer),DIMENSION(:), ALLOCATABLE:: solution
  INTEGER                          :: i,j
  INTEGER                          :: start_year,start_month,ierror
  INTEGER                          :: MOPproc,nPriorLevel, problem_size
  INTEGER                          :: begin_time, end_time
  INTEGER                          :: incSize
  CHARACTER(LEN=40),DIMENSION(5)   :: title
  CHARACTER(LEN=80)                :: msg, options
  INTEGER, DIMENSION(2)            :: stats
  INTEGER, DIMENSION(40)           :: opt_flag=0
  REAL(8),DIMENSION(9)             :: currentObjValue
  CHARACTER(LEN=32)                :: ObjId
  INTEGER,DIMENSION(9)             :: bigMlevel
  REAL(8),DIMENSION(9)             :: bigM
  CHARACTER(LEN=200),INTENT(IN)    :: runDirectory
  CHARACTER(LEN=200)               :: studyFile,traceFile,lpvarFile,stateFile,studyDir,studyName,errorLog
  CHARACTER(LEN=200)               :: dvarFile,svarFile,initvarFile
  CHARACTER(LEN=200)               :: Apart,Epart,Fpart,initfilefpart
  CHARACTER(LEN=20)                :: start_date! these are calendar years!
  CHARACTER(LEN=80)                :: basePath
  LOGICAL                          :: needToSimulate=.true.
  LOGICAL                          :: debug=.false.

  INTEGER :: period, Number_of_periods
  INTEGER :: icycle, Number_of_cycles=1

  CHARACTER(LEN=200)               ::  fileComment,hydrology,author,studydate,description,wreslFile,addXAoptions
  CHARACTER(LEN=3)               	::  startMonth
  CHARACTER(LEN=4)               	::  startYear
  CHARACTER(LEN=6)               	::  simOption,solverReport,listing
  CHARACTER(LEN=6)               	::  slackReport,saveSlackRep,winfriendly,svarReport
  CHARACTER(LEN=6)               	::  saveSvarRep,dssDebugReport,saveOldDvars,genWsiDi
  CHARACTER(LEN=6)               	::  useRestart,dumpRestart
  CHARACTER(LEN=6)               	::  posAnalysis, dialogWindow, studyType
  LOGICAL				::  showDialogWindow = .true.
  integer                         ::  bcycle,ecycle
  real                            ::  ctime,stime,setuptime,dsssavetime,dsssavecyctime,xacachetime,cycletime
  character(len=5) :: pos

  integer :: auto_solver_report = 0		!added by DE for automation of XA log when solver error such as infeas occurs
  character(LEN=8) :: report_cycle

  real ExeStartTime, ExeFinishTime

  ! DSS cache flush interval and size
  !*******************INITIALIZATION WILL NOW BE DEPENDENT ON TIME STEP******************
  INTEGER :: flush_interval
  INTEGER :: cache_back, cache_forward
  !**************************************************************************
  LOGICAL :: reload_table_log_exists
  CHARACTER(LEN=100) :: reload_table_log_file
  !-------------
  
  call cpu_time(ExeStartTime)
  ctime = 0.0
  stime = 0.0
  setuptime = 0.0
  dsssavetime = 0.0
  dsssavecyctime = 0.0
  xacachetime = 0.0
  cycletime = 0.0

  ! allocate space for the problem and solution array
  ALLOCATE (solution(maxVars))

  i=LEN_TRIM(runDirectory)
  studyFile=TRIM(runDirectory)
  traceFile=TRIM(runDirectory)
  lpvarFile=TRIM(runDirectory)
  stateFile=TRIM(runDirectory)
  errorLog =TRIM(runDirectory)
  studyFile(i+1:)="\study.sty"
  traceFile(i+1:)="\calsim.trc"
  lpvarFile(i+1:)="\lpvars.out"
  stateFile(i+1:)="\statevars.out"
  errorLog(i+1:)="\error.log"
  outputDirectory=TRIM(runDirectory)
  
!-------------------------------------
! delete previous reload table logs

      reload_table_log_file = 'log_new_tables.txt'

      INQUIRE(FILE = reload_table_log_file, EXIST=reload_table_log_exists )
      if (reload_table_log_exists ) then
        OPEN  (626, file=reload_table_log_file, status='scratch')
        close (626) 
      endif

      reload_table_log_file = 'log_reload_tables.txt'

      INQUIRE(FILE = reload_table_log_file, EXIST=reload_table_log_exists )
      if (reload_table_log_exists ) then
        OPEN  (626, file=reload_table_log_file, status='scratch')
        close (626, status='delete') 
      endif
      
      reload_table_log_file = 'log_existing_tables.txt'

      INQUIRE(FILE = reload_table_log_file, EXIST=reload_table_log_exists )
      if (reload_table_log_exists ) then
        OPEN  (626, file=reload_table_log_file, status='scratch')
        close (626, status='delete') 
      endif



!-------------------------------------
  OPEN (UNIT=12,FILE=traceFile,STATUS='REPLACE',ACTION='DENYWRITE')  ! trace file for debugging
  OPEN (UNIT=7, FILE=lpvarFile,STATUS='REPLACE',ACTION='DENYWRITE')  ! diagnostic file for non-Wresl decision variables
  OPEN (UNIT=15,FILE=stateFile,STATUS='REPLACE',ACTION='DENYWRITE')  ! diagnostic file for Wresl state variables

  ! initialize the bigMlevels and values
  DO i=1,SIZE(bigMlevel)
     bigMlevel(i)=0
     bigM(i)=0.0
  END DO

  ! read inputs from study.sty
  OPEN (UNIT=11,FILE=studyFile,STATUS='old',ACTION='READ')     ! main control file, input only
  READ (11,305) fileComment
  READ (11,305) studyName
  READ (11,305) author
  READ (11,305) studydate
  READ (11,305) description
  READ (11,305) hydrology
  READ (11,305) studyDir
  READ (11,305) studyFile
  READ (11,305) wreslFile
  READ (11,305) svarFile
  READ (11,305) dvarFile
  READ (11,305) initvarFile
  READ (11,317) Epart !ADDED
  READ (11,fmt = "(i6)") number_of_periods !ADDED - FORMAT MAY NEED TO BE CHANGED
  READ (11,300) date%day !ADDED
  READ (11,316) startMonth
  READ (11,317) startYear
  !READ (11,316) endMonth !REMOVE
  !READ (11,317) endYear !REMOVE
  READ (11,318) simOption
  READ (11,300) Number_of_cycles
  READ (11,318) solverReport
  READ (11,318) listing
  READ (11,318) slackReport
  READ (11,318) saveSlackRep
  READ (11,305) addXAoptions
  READ (11,318) svarReport
  READ (11,318) saveSvarRep
  READ (11,318) dssDebugReport
  READ (11,318) saveOldDvars
  READ (11,318) genWsiDi
  READ (11,318) useRestart
  READ (11,318) dumpRestart
  READ (11,305) Apart  ! SV DV file A part
  READ (11,305) Fpart  ! SV DV file F part  
  READ (11,305) initfilefpart
  READ (11,318) posAnalysis
  READ (11,318) dialogWindow
  READ (11,*) pos
  READ (11,318,END=10) studyType	! added var for single or multi studytype for msg dlvry. - ED 12/14/2006
10  CLOSE(11)

  date%timestep = Epart(1:4) !*************This will be used in taf_cfs conversion
  
  CALL set_flush_interval(Epart, flush_interval, cache_back, cache_forward) !************

  Apart=TRIM(Apart) !Apart="CALSIM" 
  Fpart=TRIM(Fpart) !Fpart=studyName  
  title(1)=studyName
  start_date=" "
  start_date(1:3)=startMonth
  start_date(4:7)=startYear
  MOPproc=0
  if (chareq(TRIM(simOption),"NORMAL")) MOPproc=0
  if (chareq(TRIM(simOption),"SLP")) MOPproc=1
  if (chareq(TRIM(simOption),"LGP")) MOPproc=2
  if (chareq(TRIM(simOption),"WGP")) MOPproc=3
  p = 0
  if (pos=="TRUE") p = 1

  !    /*   Options:
  !         Index	Description
  !	 1	 	Matlist option, 0..3
  !	 2	 	Solver report,  1=yes
  !	 3	 	Relaxed yes? 1=yes
  !	 4	 	Winfriendly yes?  1=yes
  
  
  !	 5	 	State variable report?  1=yes, 2=yes and save all
  !	 6	 	LP Variable (slack,surplus) report?  1=yes, 2=yes&save
  !	 7	 	Save old Dvar values? 0=no, clear them first
  !	 8	 	Activate DSS debugging? 1=yes
  !	 9	 	Activate solver (XA) debugging? 1=yes
  !	                   ( not supported by the GUI )
  !      15             Read from restart in the beginning of the run
  !      16             Generate restart at end of run
  !    */

  if (chareq(TRIM(listing),"NONE")) opt_flag(1)=0
  if (chareq(TRIM(listing),"VAR")) opt_flag(1)=1
  if (chareq(TRIM(listing),"CON")) opt_flag(1)=2
  if (chareq(TRIM(listing),"BOTH")) opt_flag(1)=3
  if (chareq(TRIM(solverReport),"TRUE")) opt_flag(2)=1
  if (chareq(TRIM(addXAoptions), "") .AND. opt_flag(2)/=1) auto_solver_report = 1
  opt_flag(3) = 0
  opt_flag(4) = 1
  if (chareq(TRIM(svarReport),"TRUE").and.chareq(TRIM(saveSvarRep),"FALSE")) opt_flag(5)=1
  if (chareq(TRIM(svarReport),"TRUE").and.chareq(TRIM(saveSvarRep),"TRUE")) opt_flag(5)=2
  if (chareq(TRIM(slackReport),"TRUE").and.chareq(TRIM(saveSlackRep),"FALSE")) opt_flag(6)=1
  if (chareq(TRIM(slackReport),"TRUE").and.chareq(TRIM(saveSlackRep),"TRUE")) opt_flag(6)=2
  if (chareq(TRIM(saveOldDvars),"TRUE")) opt_flag(7)=1
  if (chareq(TRIM(dssDebugReport),"TRUE")) opt_flag(8)=1
  if (chareq(TRIM(useRestart),"TRUE")) opt_flag(15)=1
  if (chareq(TRIM(dumpRestart),"TRUE")) opt_flag(16)=1
  if (chareq(TRIM(dialogWindow), "FALSE")) showDialogWindow = .false.
  if (debug) WRITE(12,*) opt_flag

300 FORMAT (i2)
301 FORMAT (40i1)
305 FORMAT (a200)
306 FORMAT (a7)
307 FORMAT ('Period ',i5,' of ',i5,':  Water-year date: ',i2,'/', i2,'/',i4) !*****************
309 FORMAT (/'>> ',a)
310 FORMAT (i1,f16.5)
315 format (a)
316 format (a3)
317 format (a4)
318 FORMAT (a6)
320 FORMAT ('Priority Level = ',i1,'Big M =',f16.5)
401 FORMAT ('Wresl execution:',f6.2,' s')
402 FORMAT ('Cycle',i4,' of',i4)


  ! note:  following only works with monthly time step
  ! get beginning and ending month and year from begin date and end date
  CALL getMonthYear(start_date,start_month,start_year,ierror)
  IF (ierror/=0) call errorhandler('Begin Date misspecified: '//start_date)
  
  ! create a dss path based on /Apart///BeginDate/Epart/Fpart/
  
  !*************include date%day in the following function
  basepath=getpath(Apart,date%day, start_date,Epart,Fpart)
  !******************************************************

  ! open dss files and initialize decision variable records and the XA solver
  if (showDialogWindow) then
  	call dialogInit()
  	call dialogUpdate( 2000, title(1))
  	call dialogUpdate( 4000, "Initializing...")
  	call dialogUpdate( 6000, "Press Control-Z in XA window to break")
  end if

  ! Initialize the DSS portion of the Wrangler
  call dss_set_init_fpart(initfilefpart)
  CALL dss_open(initvarfile,dvarFile,svarFile,basePath, opt_flag(8)==1)
  CALL dss_init()  ! generated by Wresl Coder

  ! Initialize the XA solver, including the debugging option and message output unit
  call solver_init( opt_flag(9)==1, 12)

  ! Announce the model banner in our log files
  WRITE(  7,315) '>> WRIMS Version ' // version_identifier
  WRITE( 12,315) '>> WRIMS Version ' // version_identifier
  WRITE( 15,315) '>> WRIMS Version ' // version_identifier
  WRITE(  7,315) title(1)
  WRITE( 12,315) title(1)
  WRITE( 15,315) title(1)
  WRITE(  7,315) copyright
  WRITE( 12,315) copyright
  WRITE( 15,315) copyright
  WRITE(  7,315)
  WRITE( 12,315)
  WRITE( 15,315)

  ! Announce the model banner in the XA log file
  call solver_message('>> WRIMS Version ' // version_identifier)
  CALL solver_message(copyright)
  call solver_message(title(1))

  ! Set basic XA command-line options from our GUI control tab
  call set_solver_options( opt_flag)

  ! Get extra XA command-line options
  call solver_message('>> These extra XA options were obtained:')
  CALL solver_options(TRIM(addXAoptions))
!  call solver_message(' ')
  call solver_message('##########################################################')

  ! Clear out the solution DSS file records, if desired
  ! Must append the day of the month to the date so that data is stored on
  ! correct day. If day is ommitted DSS stores data at first day of month
  IF( opt_flag(7) == 0) then
     call timer(begin_time)
     if (debug) WRITE(12,*) 'Clearing previous solution values'
     if (showDialogWindow) call dialogUpdate(4000, 'Clearing previous solution values')
     start_date(3: )=start_date
     date%month=start_month
     write(start_date(1:2),fmt='(i2)') date%day  
!     WRITE (*,*) 'Calling dss_clear_dvars from wrapper.f90'  !DEBUGGING
     call dss_clear_dvars( start_date, Number_of_periods)
     call timer(end_time)
     if (debug) WRITE(12,*) "Clear Solution Time: ",(end_time-begin_time)/100.
     WRITE(msg,401) (end_time-begin_time) / 100.
     if (showDialogWindow) call dialogUpdate(8000,msg)
  END if

  ! initialize date and the problem array cache
  date%wateryear = start_year
  date%month = start_month
  !date%day = start_day **************No need for this (already commented out)
!  WRITE (*,*) 'Calling rcc_cache_init from wrapper.f90'  !DEBUGGING
  call rcc_cache_init(maxRCC)
	ALLOCATE (problem(maxRcc))

  ! Main loop:  It is titled YEARLY for historical reasons, but it need not actually
  ! be a yearly loop
  YEARLY: DO period = 1, Number_of_periods
     if (period>12) then
     	p = 0
     end if   
     ! Print out the current date
     write (msg, 307) period, Number_of_periods, date%month, date%day, date%wateryear !*************
     if (debug) WRITE(12,309) msg
     call flush(12)
     if (showDialogWindow) call dialogUpdate( 2000, title(1))
     if (showDialogWindow) call dialogUpdate( 4000, msg)
     if (opt_flag(2)==1) call note_date( 1, date%day, date%month, date%wateryear) !****************

     ! Periodically read in DSS data and write out decision variables
     IF (MOD(period-1, flush_interval) == 0) THEN
        if (debug) WRITE (12,*) 'Read-flushing DSS'
        call flush(12)
        if (showDialogWindow) call dialogUpdate( 6000, 'Read-flushing DSS')
        call timer(begin_time)
!        WRITE (*,*) 'Calling dss_read_flush from wrapper.f90'  !DEBUGGING
       	if (Number_of_periods < cache_forward) then
       	!*****************SOMETHING MAY NEED TO BE DONE ABOUT THIS********************
        	CALL dss_read_flush( cache_back, Number_of_periods+cache_back, opt_flag(8)==1)
        !*****************************************************************************
        else
        	CALL dss_read_flush( cache_back, cache_forward, opt_flag(8)==1)
        end if
        call timer(end_time)
        dsssavetime = dsssavetime + (end_time-begin_time)/100.
        WRITE(msg,401) (end_time-begin_time) / 100.
        if (showDialogWindow) call dialogUpdate( 8000,msg)
     END if

     !  Set up the number of days in the time step for use by Code
     if (Epart == '1MON') then
        date%deltaT_days = days_in_month( date)
             
     !**************ADD TIME INCREMENT FOR 1 DAY*******************
     else if (Epart == '1DAY') then
     	date%deltaT_days = 1
     !*************************************************************
     
        ! date%day is undefined as of this version !*********now it is
     else
        call ERRORHANDLER("Unknown Time Step: " // Epart)
     end if

     ! Allocate and clear CYCLE cache
     if (debug) WRITE (12,*) 'Read-flushing Cycle Cache'
     if (debug) call flush(12)
     if (showDialogWindow) call dialogUpdate( 6000, 'Read-flushing Cycle Cache')
     call timer(begin_time)
!     WRITE (*,*) 'Calling cycle_read_flush from wrapper.f90'  !DEBUGGING
     call cycle_read_flush(0,Number_of_cycles,.false.)
     call timer(end_time)
     dsssavecyctime = dsssavecyctime + (end_time-begin_time)/100.
     WRITE(msg,401) (end_time-begin_time) / 100.
     if (showDialogWindow) call dialogUpdate( 8000,msg)

     DO icycle=1,Number_of_cycles ! BEGIN CYCLE LOOP
				call timer(bcycle)
        WRITE(msg,402) icycle,Number_of_cycles
        if (debug) WRITE(12,*) msg
        if (debug) call flush(12)
      	if (showDialogWindow) call dialogUpdate( 6000, msg)

      	! call generated code for all defines, goals, conditionals and rcc matrix
     	if (debug) write (12,*) 'Executing Wresl'
     	if (debug) call flush(12)
      	if (showDialogWindow) call dialogUpdate(6000,'Executing Wresl')
      	if (showDialogWindow) call dialogUpdate(8000,'')
      	if (icycle==1) then
           call rcc_cache_clear
           if (debug) write (12,*) 'Calling Code 00'
           if (debug) call flush(12)
           call timer(begin_time)
           CALL CODE(0,needToSimulate,p)
           call timer(end_time)
           cycletime = (end_time-bcycle)/100.
           if (debug) WRITE(12,*) 'Cycle 0 Time: ',cycletime
           if (debug) call flush(12)
           call timer(bcycle)
           ctime = ctime + (end_time-begin_time)/100.
           WRITE(msg,401) (end_time-begin_time) / 100.
           if (showDialogWindow) call dialogUpdate(8000,msg)
        end if
        call timer(begin_time)
       	if (debug) write (12,*) 'Calling Code ', icycle
     	if (debug) call flush(12)
      	CALL CODE(icycle,needToSimulate,p)
     	if (debug) write (12,*) 'Finished calling Code ', icycle
     	if (debug) call flush(12)
        call timer(end_time)
        ctime = ctime + (end_time-begin_time)/100.
        WRITE(msg,401) (end_time-begin_time) / 100.
        if (showDialogWindow) call dialogUpdate(8000,msg)

      	! check to see if they want out
      	if (showDialogWindow) then
      		IF (dialogQuitRequest()) EXIT YEARLY
      	end if

      	! 3 solution procedures possible: 0=normal/no MOP, 1=sequential/constrain MOP method, 2=weighted MOP GP method
      	CurrentOBJvalue(1) = 0.
	
      	IF (MOPproc==0) THEN                         ! NO MULTIOBJECTIVE PROGRAMMING - default option
           if (debug) write (12,*) 'Entering RCC_SETUP'
           if (debug) call flush(12)
           if (showDialogWindow) call dialogUpdate(6000, 'Entering RCC_SETUP')
           ALLOCATE (problem( rcc_cache_size()))
           problem_size =  RCC_SETUP(problem)
           call solver_pose(problem(1:problem_size))
           if (debug) write (12,*) 'Solving'
           if (debug) call flush(12)
           if (showDialogWindow) call dialogUpdate( 6000, 'Solving')
           stats = solver(solution)
           IF (stats(1) > 2.or.stats(2)/=1) THEN
              if (debug) write (12,*) 'Exiting Yearly loop'
              if (debug) call flush(12)
              EXIT YEARLY
           END IF

      	ELSE IF (MOPproc==1) THEN                        ! CYCLES - SEQUENTIAL LINEAR PROGRAMMING
           if (debug) write (12,*) 'Entering CYCLE_CON_SETUP'
           if (debug) call flush(12)
           if (showDialogWindow) call dialogUpdate( 6000, 'Entering CYCLE_CON_SETUP')
           if (debug) write (12,*) 'Getting Problem'
           if (debug) call flush(12)
           call timer(begin_time)
           if (needToSimulate) then
              IF (Number_of_cycles > 1) THEN
                 problem_size =  CYCLE_CON_SETUP(icycle,problem)
              ELSE
                 problem_size =  RCC_SETUP(problem)
              END IF
           end if
           call timer(end_time)
           setuptime = setuptime + (end_time-begin_time)/100.

           if (debug) write (12,*) 'Solving'
           if (debug) call flush(12)
           if (debug) write (12,*) problem_size
           if (debug) call flush(12)
           if (showDialogWindow) call dialogUpdate( 6000, 'Solving')
           if (needToSimulate) call solver_pose(problem(1:problem_size))
           call timer(begin_time)
           if (needToSimulate) stats = solver( solution)
           call timer(end_time)
           stime = stime + (end_time-begin_time)/100.
           IF (stats(1) > 2.or.stats(2)/=1) THEN
              EXIT YEARLY
           END IF

      	ELSE IF (MOPproc==2) THEN                   ! LEXICOGRAPHIC GOAL PROGRAMMING - VERIFY WORKING ???
           incSize = nPriorLevel -1
           ALLOCATE( problem( rcc_cache_size() + incSize))
           DO j=1,nPriorLevel
              if (debug) write (12,*) 'entering MOP_SEQ_SETUP: Priority Level =',j
              if (showDialogWindow) call dialogUpdate( 6000, 'entering MOP_SEQ_SETUP')
              problem_size = MOP_SEQ_SETUP(j,CurrentOBJvalue,problem)
              if (debug) write (12,*) 'Solving'
             if (showDialogWindow) call dialogUpdate( 6000, 'Solving')
              call solver_pose( problem)
              stats = solver( solution)
              WRITE (ObjId,FMT='("OBJ", i1)') j
              currentObjValue(j) = getSolutionValue(ObjId)
              if (debug) WRITE(12,*) 'Current OBJ name: ',ObjId,' value = ', currentObjValue(j)
              if (debug) call flush(12)
              if (debug) write(12,*) 'Global OBJ value = ',solution(1)%value
              if (debug) call flush(12)
              IF (stats(1) > 2.or.stats(2)/=1) THEN
                 EXIT YEARLY
              END IF
              if (debug) write (12,*) 'Sequential multiobjective programming priority ',j,' complete'
              if (debug) call flush(12)
           END DO
           if (debug) write (12,*) 'Sequential multiobjective programming procedure completed'
           if (debug) call flush(12)

      	ELSE IF (MOPproc==3) THEN                   ! WEIGHTED GOAL PROGRAMMING - VERIFY WORKING ???
           incSize = nPriorLevel
           if (debug) write (12,*) 'entering MOP_WGT_SETUP'
           if (showDialogWindow) call dialogUpdate( 6000, 'entering MOP_WGT_SETUP')
           ALLOCATE(problem( rcc_cache_size() + incSize))
           problem_size = MOP_WGT_SETUP(bigMlevel,bigM,problem)
           if (debug) write (12,*) 'Solving'
           if (showDialogWindow) call dialogUpdate( 6000, 'Solving')
           CALL solver_pose( problem(1:problem_size))
           stats = solver(solution)
           CurrentOBJvalue(1)=solution(1)%value
           if (debug) write (12,*) 'OBJ = ',CurrentOBJvalue(1)
           if (debug) call flush(12)
           IF (stats(1) > 2.or.stats(2)/=1) THEN
              EXIT YEARLY
           END IF
           if (debug) write (12,*) 'Weighted multiobjective programming procedure completed'
           if (debug) call flush(12)
      	ELSE
           CLOSE(12)
           call ERRORHANDLER("Unknown solver procedure!")
      	END IF

        if (debug) write (12,*) 'Saving Cycle Solution'
        if (debug) call flush(12)
        if (opt_flag(6)==1) REWIND( 7)
        call timer(begin_time)
      	IF (icycle < Number_of_cycles) &
             call save_solution(solution(1:solver_get_solution_size()), 7, &
             date%day, date%month, date%wateryear, opt_flag(6),2, debug) !*************************
             if (debug) write (12,*) 'Saved Cycle Solution'
             if (debug) call flush(12)
        call timer(end_time)
        xacachetime = xacachetime + (end_time-begin_time)/100.
      	if (debug) write (12,*) 'Writing Statevars Report and XA Log'
      	if (opt_flag(5)==1) REWIND( 15)
      	if (opt_flag(5)>=1 .and. needToSimulate) call reportsv(icycle)  !CB uncommented and added needToSimulate part
      	if (opt_flag(2)==1 .and. needToSimulate) call solver_report  !CB added needToSimulate part
      	if (debug) write (12,*) 'Incrementing Cycle'
      	if (debug) call flush(12)
      	if (icycle < Number_of_cycles) CALL dss_inc_cycle
       	call timer(ecycle)
        cycletime = (ecycle-bcycle)/100.
        if (debug) WRITE(12,*) 'Cycle ',icycle,' Time: ',cycletime
        if (debug) call flush(12)
     END DO         ! END CYCLE LOOP

     if (debug) write (12,*) 'Writing solution to DSS and log file'
     if (debug) call flush(12)
     if (showDialogWindow) call dialogUpdate( 6000, 'Writing solution to DSS and log file')
     if (opt_flag(6)==1) REWIND(7)
     call timer(begin_time)
     if (debug) write (12,*) 'Saving Solution after cycle loop completed'
     if (debug) call flush(12)
     call save_solution(solution(1:solver_get_solution_size()), 7, &
        date%day, date%month, date%wateryear, opt_flag(6),1, debug) !******************************
     if (debug) write (12,*) 'Saved Solution after cycle loop completed'
     if (debug) call flush(12)
     call timer(end_time)
     xacachetime = xacachetime + (end_time-begin_time)/100.
     WRITE(msg,401) (end_time-begin_time) / 100.
     if (showDialogWindow) call dialogUpdate( 8000,msg)
     if (debug) write (12,*) 'Month complete'
     if (debug) call flush(12)

     ! check to see if they want out
     if (showDialogWindow) then
     	IF (dialogQuitRequest()) EXIT YEARLY
     end if

     ! increment time
     if (debug) WRITE(12,*) 'DSS Timestep increment'
     if (debug) call flush(12)
     CALL dss_inc_date
     if (debug) WRITE(12,*) 'Simulation Timestep increment'
     if (debug) call flush(12)
     
     !**********increment_date was modified in wrapper_utils
     call increment_date( date, Epart)
     !******************************************************
     
     if (debug) WRITE(12,*) 'End of main loop reached'
     if (debug) call flush(12)
  END DO YEARLY

	if (debug) WRITE(12,*) 'Code Time: ', ctime
	if (debug) WRITE(12,*) 'Setup Time: ', setuptime
	if (debug) WRITE(12,*) 'Solve Time: ', stime
	if (debug) WRITE(12,*) 'DSS Save Time: ', dsssavetime
	if (debug) WRITE(12,*) 'DSS Save Cycle Time: ', dsssavecyctime
	if (debug) WRITE(12,*) 'XA Cache Time: ', xacachetime
	if (debug) call flush(12)

  ! Save solution if bad result (for diagnosis)
  IF (stats(2)/=1 .or. stats(1)/=1) THEN
     if (opt_flag(6)==1) REWIND( 7)
     call save_solution(solution(1:solver_get_solution_size()), 7, &
     date%day, date%month, date%wateryear, opt_flag(6),1, debug) !************************
     if (opt_flag(5)==1) REWIND( 15)
     !if (opt_flag(5)>=1) call reportsv(icycle)
  END IF
  close (7)
  CLOSE (10)

  !DE automatically log solver errors when no other XA options requested
  IF (stats(2) /= 1 .or. (stats(1) /= 1 .and. stats(1) /= 2)) then
	if (auto_solver_report == 1) then
!		call solver_message('')
  		call solver_message('##########################################################')
!		call solver_message('')
		call solver_options ('MATLIST VAR MPSX YES MUTE NO LISTINPUT NO')
!		call solver_message('')
		call note_date(1, date%day, date%month, date%wateryear)
		write(report_cycle, fmt="('CYCLE ', i2)") icycle
		call solver_message (report_cycle)
!		call solver_message('')
		if (needToSimulate) call solver_pose(problem(1:problem_size))
		if (needToSimulate) stats = solver( solution)
	end if
  end if

  ! close everything and finish
  DEALLOCATE(problem)
  call rcc_cache_destroy
  call solver_close !CB re-inserted it (after it was removed from 1.3 beta for some reason)
  if (debug) WRITE(12,*) 'DSS close'
  if (debug) call flush(12)
  
  if (showDialogWindow) call dialogUpdate( 4000, 'Finishing up')
  if (showDialogWindow) call dialogUpdate( 6000, 'Flushing to DSS')
  if (showDialogWindow) call dialogUpdate( 8000, '')
  CALL dss_close
  DEALLOCATE( solution)
  if (showDialogWindow) call dialogRemove()

747 FORMAT(a, f7.1, a)

  ! final message indicating normal completion - solution errors terminate before here
  IF (stats(2)==1 .and. (stats(1)==1 .or. stats(1)==2) ) THEN
     call cpu_time(ExeFinishTime)
     write(msg,747) 'Computation Time: ', ExeFinishTime-ExeStartTime, ' Seconds'
     if (showDialogWindow) then
         IF (chareq(TRIM(genWsiDi),"FALSE").and.chareq(TRIM(posAnalysis),"FALSE"))  CALL StopWithFinal(msg)
     end if    
  ELSE
     CALL XA_ERRORHANDLER(stats(1),stats(2),date,icycle,Number_of_cycles,studyType,errorLog)
     msg='XA HAD BAD SOLUTION'
  END IF

  if (debug) write (12,*) 'Wrapper program complete with ',msg
  if (debug) call flush(12)
  CLOSE(12)

END SUBROUTINE WRAPPER
