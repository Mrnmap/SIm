simLAB installation under Linux
  $ sudo apt-get install python-pip
  $ sudo apt-get install python-dev
  $ sudo apt-get install python-tk
  $ sudo apt-get install swig
  $ sudo apt-get install libpcsclite1 pcscd pcsc-tools
  $ sudo apt-get install python-pyscard
  $ sudo easy_install pyusb lxml
  $ sudo pip install plac gevent zerorpc
  $ git clone https://github.com/kamwar/simLAB.git
  $ cd ./simLAB
  $ sudo python simlab.py

simLAB installation under Microsoft Windows 7
Install python 2.7.11 64bit
 Add C:\Python27 to system enviroment variable PATH
Install swig https://sourceforge.net/projects/swig/files/swigwin/swigwin-3.0.8/swigwin-3.0.8.zip/download
 Download and extract, add directory containing swig.exe to system enviroment variable PATH
Install Microsoft Visual C++ 9.0 for python. Download from http://aka.ms/vcpython27
Install python packages: pyusb, plac, gevent, zerorpc, lxml:
$ cd C:\Python27\Scripts
$ pip install pyusb plac gevent zerorpc

There are problems with installing lxml by pip, use easy_install instead
$ cd C:\Python27\Scripts
$ easy_install lxml

Install pyscard, use e.g. git-scm for windows
$ git clone https://github.com/LudovicRousseau/pyscard.git
$ cd ./pyscard/
$ python setup.py build_ext install

Note: don't use "pip install pyscard", ../smartcard/scard/scard.py will be missing
Download libusb from https://sourceforge.net/projects/libusb/files/libusb-1.0/
 Copy .\MS64\dll\libusb-1.0.dll to C:\Python27
 If simTrace HW is connected, run .\libusb-win32-bin-1.2.6.0\bin\inf-wizard and install driver for AT91USBSserial
Download and run simLAB, use e.g. git-scm for windows
  $ git clone https://github.com/kamwar/simLAB.git
  $ cd ./simLAB
  $ python simlab.py

Microsoft Windows Installation issues
 Gevent installation error: Setup script exited with error: Unable to find vcvarsall.bat. Depending on the Visual studio installer, set in terminal
Visual Studio 2010 (VS10):  SET VS90COMNTOOLS=%VS100COMNTOOLS%  
Visual Studio 2012 (VS11):  SET VS90COMNTOOLS=%VS110COMNTOOLS%  
Visual Studio 2013 (VS12):  SET VS90COMNTOOLS=%VS120COMNTOOLS%

Verifying environment setup
Before you begin, you can verify that your environment has been correctly setup. Insert live SIM (e.g. Mobile Operator SIM or test UICC SIM) into PC/SC compliant reader.
Start test runner to execute internal tests for both live and soft SIM.
$ cd /path/to/simLAB
$ python ./tests/runner.py

Test results example
test UICC SIM - security codes are known
operator SIM - security codes are unknown
Example
This is just a quick example of simLAB capabilities as a SIM editor.
readi - read EF_IMSI value (interpreted)
/>readi EF_IMSI
status OK
data 001010123456789

writei - update EF_IMSI value (interpreted)
/>writei EF_IMSI 001020123456789
status OK

get_plmn - get HPLMN (based on IMSI)
/ADF0/6F07>get_plmn
status OK
data 00102

set_plmn - update HPLMN with MCC=310, MNC=410
/ADF0/6FAD>set_plmn 310410
status OK

read - read EF_AD raw data to verify length of MNC in the IMSI
/ADF0/6F07>read EF_AD
status OK
data 80000103
 
