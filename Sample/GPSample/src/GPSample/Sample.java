/**
 * 
 */
package GPSample;

import javacard.framework.ISO7816; 
import javacard.framework.ISOException; 
import javacard.framework.Util; 
import javacard.framework.Applet; 
import javacard.framework.APDU; 
import javacard.framework.JCSystem; 
 
import org.globalplatform.*; 
 
 
public class Sample extends Applet {  
         
    // Commands the applet implements 
    final static byte CLA_Proprietary = (byte) 0x80; 
    final static byte CLA_Proprietary_Secured = (byte) 0x84; 
    final static byte CLA_ISO7816_Secured = (byte) 0x04; 
    final static byte INS_VERIFY = (byte) 0x20; 
    final static byte INS_CHANGE_PIN = (byte) 0x24; 
    final static byte INS_PUT_KEY = (byte) 0xD8; 
    final static byte INS_GET_STATUS = (byte) 0xF2; 
    // Mask the secure messaging bit 3 of the Class byte 
    final static byte SM_MASK = (byte) 0xFB; 
    // Constants for the applet¡¯s Select response string  
    final static byte FCI_Template = (byte) 0x6F; 
    final static byte AID_Tag = (byte) 0x84; 
    final static byte Proprietary_Data_Template = (byte) 0xA5; 
    // Application specific Life Cycle State of the applet 
    final static byte APPLET_PERSONALIZED = (byte) 0x0F; 
 
    // This application example makes use of the Global PIN provided by the GlobalPlatform 
    // CVM Interface. The CVM State keeps track of whether the PIN has been presented 
    // correctly from one APDU to the next. For this example it is explicitly reset 
    // each time the application is selected, regardless if it was previously presented 
    // (correctly or not) during the current card session.     
    final static byte CVM_Management_Privilege = (byte) 0x02; 
    private CVM MyPIN; 
    
    // The next statements are only required if Security Domain features are being used by
    // the application. The MySecureChannel is a handle to this application's associated 
    // Security Domain.   
    private SecureChannel MySecureChannel; 
    private short responseLength; 
    private short wrappedLength; 
     
    /*  Applet constructor 
     *      The constructor for the applet would typically be where objects to be 
     *      used by the applet would be instantiated but not necessarily populated. 
     *      For the purpose of this example code, the only function of the constructor 
     *      is to register itself with the instance AID defined by the off-card 
     *      entity. 
     *      @ Parameters 
     *          buffer :- buffer containing at least the instance AID and application 
     *                  privileges. 
     *          offset :- offset within the buffer pointing to the beginning of the 
     *                  instance AID length. 
     *          length :- length of the install parameters within the buffer.                      
    **/ 
    protected Sample(byte[] buffer, short offset, byte length) {       
     
        // Determine the length of the instance AID. 
        byte instanceLength = (byte) buffer[offset]; 
        // Locate the install privileges. 
        byte appletPrivileges = (byte) buffer[(short) (offset+1+instanceLength+1)]; 
 
        // Ensure that the applet has CVM Management permission. 
        if ( appletPrivileges != CVM_Management_Privilege ) 
            ISOException.throwIt(ISO7816.SW_DATA_INVALID); 
   
        // Ensure that the card implements the CVM Interface  
        MyPIN = GPSystem.getCVM(GPSystem.CVM_GLOBAL_PIN); 
        if ( MyPIN == (CVM) null ) 
            ISOException.throwIt(Util.makeShort((byte) 0x6A, (byte) 0x88)); 
             
        register(buffer, (short)(offset + 1), instanceLength); 
    } 
   
    /*  Install method  
     *      As defined in the Java Card specifications and invoked by the JCRE on receipt 
     *      of an INSTALL [for instal]l command as defined in GlobalPlatform specifications 
     *      Parameters 
     *          buffer :- buffer containing at least the length and values of the 
     *                  following: instance AID, application privileges and 
     *                  application specific parameters    
     *          offset :- offset within the buffer pointing to the beginning of the 
     *                  install parameters i.e. instance AID length. 
     *          length :- length of the install parameters within the buffer.          
     *      Note that this example applet does not utilize application specific parameters. 
    **/ 
    public static void install(byte[] buffer, short offset, byte length) {         
                                                    
        // Applet constructor 
        new Sample(buffer, offset, length); 
    }
    
    /*  Select method
    *      As defined in the Java Card specifications and invoked by the JCRE on receipt 
     *      of a SELECT command indicating this applet's instance. 
     *      Please note that it is advised that, for Global Platform, minimal code, if any, 
     *      be contained in this method and that for no reason should this method ever fail 
     *      or return false rather than true. 
    **/ 
    public boolean select() {  
         
        // Retrieve the handle of the Security Domain associated with this applet. 
        MySecureChannel = GPSystem.getSecureChannel();
    
        // Reset the CVM State of the Global PIN, regardless if the PIN has been previously 
        // presented (correctly or not) during the current card session. This example  
        // requires that the PIN be systematically (re)presented to the application.   
        MyPIN.resetState(); 
        return true;     
    }    
 
 
    /* Deselect method 
     * calling resetSecurity() will ensure that if a Secure Channel Session 
     * has been established, it will not be available to another application. 
     * If a Secure Channel Session has not been established, this call will have no 
     * effect 
    */  
     public void deselect() { 
         MySecureChannel.resetSecurity(); 
     } 
 
 
    /*  Process method  
     *      As defined in the Java Card specifications and invoked by the JCRE on  
     *      receipt of a command intended for the applet. The applet is responsible 
     *      for processing or dispatching each command. 
     *      Parameters 
     *          apdu :- the apdu buffer. 
    **/   
    public void process( APDU apdu ) {  
     
        // Generally commands are dispatched within an application according to 
        // their class and instruction bytes. Secure messaging is checked by 
        // individual methods. 
        byte[] buffer = apdu.getBuffer();      
        byte cla = (byte) (buffer[ ISO7816.OFFSET_CLA ] & (byte) SM_MASK); 
         
        // ISO class             
        if( cla == ISO7816.CLA_ISO7816 ){ 
            switch ( buffer[ISO7816.OFFSET_INS] ) { 
            // SELECT Command 
            case (byte) ISO7816.INS_SELECT: 
            	processSelect( apdu ); 
                break; 
            // PRESENT PIN command 
            case (byte) INS_VERIFY: 
            	presentPIN( apdu ); 
                break; 
            default: 
            	ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED); 
                break; 
            } /* end switch */ 
            return; 
        }    
         
        // Proprietary class commands 
        if( cla == CLA_Proprietary ){      
            switch ( buffer[ISO7816.OFFSET_INS] ) { 
            // PUT KEY command 
            case (byte) INS_PUT_KEY: 
            	personalize( apdu ); 
                break; 
            // CHANGE PIN command 
            case (byte) INS_CHANGE_PIN: 
            	changePIN( apdu ); 
                break; 
            // GET STATUS command 
            case (byte) INS_GET_STATUS: 
            	getStatus( apdu ); 
                break; 
            // Command not directly supported by the applet: it might be a Secure Channel
            // Protocol command 
            default: 
            	SCPcommands( apdu ); 
                break; 
            } /* end switch */ 
            return; 
        }   
        ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED); 
    } 
 
 
   /*  processSelect method 
     *      Method to handle the SELECT command.  
     *      Parameters 
     *          apdu :- the apdu buffer. 
    **/   
    void processSelect( APDU apdu ) { 
         
        // As the JCRE dispatches SELECT commands which cannot be accessed through the 
        // registry to the currently selected applet, the applet must insure it is being 
        // selected prior to responding. 
        if( !selectingApplet() ) {         
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED); 
        } 
         
        // Even though the command message has already been received by the JCRE, for 
        // consistency a pseudo receive is performed. 
        apdu.setIncomingAndReceive();     
        byte[] buffer = apdu.getBuffer(); 
                        
        // Retrieve the complete AID of the selected applet and build the Select 
        // response string.
        short offset = ISO7816.OFFSET_CDATA; 
        short AIDLength = JCSystem.getAID().getBytes(buffer, (short)(offset+4)); 
        buffer[offset] = (byte) FCI_Template ; 
        buffer[(short)(offset+1)] = (byte)(4+AIDLength); 
        buffer[(short)(offset+2)] = (byte) AID_Tag; 
        buffer[(short)(offset+3)] = (byte) AIDLength; 
        buffer[(short)(offset+4+AIDLength)] = (byte) Proprietary_Data_Template; 
        buffer[(short)(offset+4+AIDLength+1)] = (byte) 0x00; 
        apdu.setOutgoingAndSend( (short) ISO7816.OFFSET_CDATA, (short) (6+AIDLength) ); 
   } 
 
    
   /*  SCPcommands method 
    *       Method to handle the Secure Channel Protocol commands defined in the  
    *       GlobalPlatform specifications, such as: INITIALIZE UPDATE and EXTERNAL 
    *       AUTHENTICATE commands.  
    *       The processing for these commands is performed by the Security Domain  
    *       associated with the application.  
    *       Errors are managed and output by the Security Domain. 
    *       Parameters 
    *          apdu :- the apdu buffer. 
   **/  
   void SCPcommands ( APDU apdu ) { 
         
        // The processSecurity() method proceeds to receive the APDU and expect it as  
        // defined in the GlobalPlatform specifications. The method will throw an error if  
        // the command is not recognized. 
        // The prepared response message is placed in the APDU buffer by the Security 
        // Domain in position 5. The length to be output is returned by the processSecurity  
        // method. 
        responseLength = MySecureChannel.processSecurity( apdu ); 
        if (responseLength != 0 ) 
            apdu.setOutgoingAndSend( (short) ISO7816.OFFSET_CDATA, responseLength );           
    }
   
   /*  personalize method 
    *       Method to handle the personalization (PUT KEY) command.  
    *       The personalization command requires secure messaging and contains 
    *       one single PIN with HEX formated. 
    *       Parameters 
    *          apdu :- the apdu buffer. 
    **/  
    void personalize( APDU apdu ) {     
        
       // The applet checks its own state to insure it is not already personalized. 
       if( GPSystem.getCardContentState() != GPSystem.APPLICATION_SELECTABLE ) 
           ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED); 
            
       byte[] buffer = apdu.getBuffer();            
       
       // The personalization data is retrieved and the command is unwrapped by the 
       // Security Domain. 
       // Errors in the unwrapping process are managed and output by the Security Domain. 
       wrappedLength = apdu.setIncomingAndReceive();               
       MySecureChannel.unwrap(buffer, (short) ISO7816.OFFSET_CLA,  
                                                              (short) (wrappedLength+5)); 

       byte pinLen = buffer[ISO7816.OFFSET_LC];
       MyPIN.update(buffer, (short)(ISO7816.OFFSET_CDATA), pinLen, CVM.FORMAT_HEX);
       
       // As the application now contains personalization data, it's Life Cycle 
       //  State transitions to its own specific state ¡°Personalized¡±. 
       GPSystem.setCardContentState( APPLET_PERSONALIZED );
       
   } 

    
   /*  getStatus method 
    *       Method to handle the Get Status command.  
    *       The Get Status command may or may not include secure messaging and returns 
    *       the status of the application to the off-card entity. 
    *       Parameters 
    *          apdu :- the apdu buffer. 
   **/ 
   void getStatus( APDU apdu ) {                
      
       byte[] buffer = apdu.getBuffer(); 
        
       // Only if the command includes secure messaging will it contain a command 
       // message i.e. a MAC. In which case the command message must be retrieved 
       // and the command unwrapped. 
       // Errors in the unwrapping process are managed and output by the Security 
       // Domain. 
       if( buffer[ ISO7816.OFFSET_CLA ] == CLA_Proprietary_Secured ){ 
           wrappedLength = apdu.setIncomingAndReceive();               
           MySecureChannel.unwrap(buffer, (short) ISO7816.OFFSET_CLA,  
                                                              (short) (wrappedLength+5)); 
       }
       
    // The application Life Cycle State is retrieved and output to the external entity. 
       buffer[ISO7816.OFFSET_CDATA] = GPSystem.getCardContentState();      
       apdu.setOutgoingAndSend( (short) ISO7816.OFFSET_CDATA, (short) 1 ); 
   } 


   /*  presentPIN method 
    *       Method to handle the present PIN command.  
    *       The present PIN command checks that the PIN presented by the external entity 
    *       to the card is valid. As this is the global PIN, the applet assumes that 
    *       the PIN has been previously populated. 
    *       Parameters 
    *          apdu :- the apdu buffer. 
   **/ 
   void presentPIN( APDU apdu ) {         
        
       // PIN presentation can only occur when the application is in its own state  
       // ¡°Personalized¡±. 
       if( GPSystem.getCardContentState() != APPLET_PERSONALIZED ) 
           ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED); 
            
       // If the CVM State of the PIN is blocked, i.e. its retry limit has been reached,  
       // an appropriate error is thrown. 
       if( MyPIN.isBlocked() ) 
            ISOException.throwIt(Util.makeShort( (byte) 0x69, (byte) 0x83)); 
  
       // Retrieve the PIN data and unwrap the command if secure messaging is present. 
       byte[] buffer = apdu.getBuffer(); 
       wrappedLength = apdu.setIncomingAndReceive();               
       if( buffer[ ISO7816.OFFSET_CLA ] == CLA_ISO7816_Secured ) 
           MySecureChannel.unwrap(buffer, (short) ISO7816.OFFSET_CLA,  
                                                              (short) (wrappedLength+5)); 

       // The applet assumes that the PIN formatting required by the verify() method has 
       // been applied by the off-card entity and uses the ¡®transparent¡¯ HEX format. 
       MyPIN.verify(buffer, (short) ISO7816.OFFSET_CDATA, buffer[ISO7816.OFFSET_LC], 
                                                                        CVM.FORMAT_HEX); 
            
       // If PIN verification failed, an appropriate error indicating how many 
       // opportunities remain to present the correct PIN will be thrown. 
       if ( !MyPIN.isVerified() ) { 
           byte triesRemaining = (byte) (MyPIN.getTriesRemaining () | (byte) 0xc0 );                         
                
           ISOException.throwIt(Util.makeShort( (byte) 0x63, triesRemaining )); 
       }   
   } 
   

   /*  changePIN method 
    *       Method to handle the change PIN command.  
    *       The change PIN command allows the Global PIN to be changed if the correct 
    *       PIN has been previously presented.  
    *       Parameters 
    *          apdu :- the apdu buffer. 
   **/   
   void changePIN( APDU apdu ) {       
        
       // The PIN must have been previously presented. 
       if ( !MyPIN.isVerified() ) 
               ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);     
 
       // Retrieve the command data 
       byte[] buffer = apdu.getBuffer(); 
       wrappedLength = apdu.setIncomingAndReceive();               
       // unwrap if secure messaging is present 
       if( buffer[ ISO7816.OFFSET_CLA ] == CLA_Proprietary_Secured )  
           MySecureChannel.unwrap(buffer, (short) ISO7816.OFFSET_CLA,  
                                                              (short) (wrappedLength+5)); 

       // The applet assumes that the correct PIN formatting has been applied by the  
       // off-card entity and uses the ¡®transparent¡¯ HEX format. 
       MyPIN.update(buffer, (short) ISO7816.OFFSET_CDATA, buffer[ISO7816.OFFSET_LC], 
                                                                        CVM.FORMAT_HEX); 
   }
   
} // end applet 