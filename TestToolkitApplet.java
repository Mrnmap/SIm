//-----------------------------------------------------------------------------
//	TestToolkitApplet.java
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
//	Package Definition
//-----------------------------------------------------------------------------
package org.etsi.tests.api;


//-----------------------------------------------------------------------------
//	Imports
//-----------------------------------------------------------------------------
import sim.toolkit.* ;
import javacard.framework.* ;


/**
 *
 * Description of the area od test (e.g. Test of the method getLength on the ProactiveHandler).
 * The default test applet is triggered on a formatted SMS PP reception.
 * The result of each and every test case is stored in an array.
 * The results of the test can be obtained by selecting the test applet : 
 * the applet ID and the test cases results are then returned in the response data.
 *
 * @version 0.0.3 - 22/06/00
 * @author SA
 *
 */
public class TestToolkitApplet extends javacard.framework.Applet implements ToolkitInterface, ToolkitConstants
{

	/*
	 * Generic variables for all test applets
	 */

	/**	baSIMAppletAID	byte array containing the identifier of the SIM applet */
	protected byte []	baSIMAppletAID = {(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x09,(byte)0x00,(byte)0x01} ;


	/**	obReg			ToolktRegistry object */
	protected ToolkitRegistry obReg ;


	/**	
	 *	baTestAppletId	byte array to store the identifier of the test applet, 
     *				    the first byte is the length of the AID. 
	 */
	protected byte []	baTestAppletId = new byte [17] ;

	/**	
	 *	baTestsResults	byte array containing the result of each and every test case, 
	 *				    the first byte is the number of test cases.
	 */
	protected byte []	baTestsResults = new byte [128] ;


	/**
	 * Constructor of the applet
	 */
	public TestToolkitApplet() 
	{
		// Register to the SIM Toolkit Framework
		obReg = ToolkitRegistry.getEntry() ;

		// Set Formatted SMS PP as an activation event for the applet
		obReg.setEvent(EVENT_FORMATTED_SMS_PP_ENV);
    	}


	/**
	 * Method called by the JCRE at the installation of the applet
	 */
	public static void install(byte bArray[], short bOffset, byte bLength) 
	{
		// Create a new applet instance
		TestToolkitApplet ThisApplet = new TestToolkitApplet();

		// Register the new applet instance to the JCRE			
       	ThisApplet.register();

		// Get the AID value 
		baTestAppletId[0] = JCSystem.getAID().getBytes(baTestAppletId, (short)0x0001);
	}


	/**
	 * Method called by the JCRE Framework
	 *
	 * @param clientAID the AID of the calling application, should be GSM application
	 * @param parameter not used for the moment
	 * What about putting this (and the baSIMAppletAID byte array) in the ToolkitApplet definition ?
	 */
	public Shareable getShareableInterfaceObject ( AID clientAID, byte parameter) 
	{
		/*
		 *	Check the AID of the client applet to grant access 
		 *	to the SIM applet only
		 */
		if ( clientAID.partialEquals(baSIMAppletAID, (byte) 0x00, (byte) baSIMAppletAID.length) == true )
		{
			return ((Shareable) this);
		}
		else
		{
			return(null);
		}
	}


	/**
       * Method called by the SIM Toolkit Framework
	 */
	public void processToolkit(byte event) 
	{
		byte TestCaseNb = (byte) 0x00 ;

		switch(event) 
		{
			case EVENT_FORMATTED_SMS_PP_ENV :

				/*
				 * All the test cases are processed sequentially
				 * the sequence of test is not interrupted by the 
				 * failure : if the card is not compliant to a given
				 * test case, the test sequence will continue to 
				 * process the following test cases.
				 * Each and every test cases must be included in a try catch statement.
				 * Note : Test test case numbering begin at 1 (number 0
				 *        is reserved to store the total number of test cases)
				 */

				/** Test Case 0 : description of the test case */
				TestCaseNb = (byte) 0x01 ;

				/* Card Compliance Test */
				try
				{
					if (true)
					{
						reportTestSuccess(TestCaseNb);
					}
					else
					{
						reportTestFailure(TestCaseNb);
					}						
				}
				catch (Exception e)
				{
					reportTestFailure(TestCaseNb);
				}


				// ...


				/** Test Case 127 (maximal number of test cases in a test applet) */
				TestCaseNb = (byte) 0x7F ;

				/* Card Compliance Test */
				try
				{
					if (true)
					{
						reportTestSuccess(TestCaseNb);
					}
					else
					{
						reportTestFailure(TestCaseNb);
					}						
				}
				catch (Exception e)
				{
					reportTestFailure(TestCaseNb);
				}

				// End of the Test Sequence
				break ;


			default :
				break ;
            }
	}


	/**
	 *	
	 */
	protected void reportTestSuccess(byte TestCaseNb)
	{
		// Update the total number of tests passed
		baTestsResults[0] = TestCaseNb ;

		// Set the Test Case Result byte to 0xCC (for Card Compliant..)
		baTestsResults[TestCaseNb] = (byte)0xCC ;
	}

	/**
	 *	
	 */
	protected void reportTestFailure(byte TestCaseNb)
	{
		// Update the total number of tests passed
		baTestsResults[0] = TestCaseNb ;

		// The default value of the byte array is 0x00, so there is no need 
		// to update the value of the Test Case Result byte to 0x00.
	}


	/**
	 * Method called by the JCRE, once selected
	 * This method allows to retrieve the detailed results of the previous execution
	 * may be identical for all tests
	 */
	public void process(APDU apdu) 
	{
		if (selectingApplet())
		{
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)(baTestAppletId[0] + 1 + baTestsResults[0] + 1));


			// Send the applet ID byte array
			apdu.sendBytesLong(baTestAppletId, (short)0, (short)(baTestAppletId[0] + 1));

			// Send the tests results byte array
			apdu.sendBytesLong(baTestsResults, (short)0, (short)(baTestsResults[0] + 1));
		}
	}
}
