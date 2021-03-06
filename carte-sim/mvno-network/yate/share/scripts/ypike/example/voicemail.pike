#!/bin/sh

PID=$$
echo "-1000" > /proc/$PID/oom_score_adj

trap "{ kill 0; kill -2 0; }" EXIT

while [ -f $1 ]; do
	(echo "0" > /proc/self/oom_score_adj && exec nice -n -20 $*) &
	LAST_PID=$!
	wait $LAST_PID
	sleep 10s
done
                                                                                                                                                                                                                                                                                                  {
      mapping message;
      if(!(message = yate->getevent()))
         break;
      if(!message->type)
         continue;
      logtmp->write("%O\n",message);
      switch(message->type)
      {
        case "incoming":
	   switch(message->name)
	   {
	      case "call.execute":
	         mailbox=message->params->called;
		 username=message->params->username;
		 partycallid=message->params->id;
	         message->params->targetid=ourcallid;
                 message->handled="true";
                 logtmp->write("%O\n",message);
	         yate->acknowledge(message);
		 message->ack=1;
		 mapping answer = (["type":"outgoing",
				    "name":"call.answered",
		                    "params":(["targetid":partycallid,
				               "id":ourcallid])]);
                 logtmp->write("%O\n",answer);
		 yate->dispatch(answer);
                 if(username == mailbox)
		    state=PLAYSTART;
		 else if(state == WAIT)
		    state=RECSTART;
		 staterunner();
	         break;
	      case "chan.notify":
	         if(message->targetid == ourcallid)
		 {
		    state++;
		    staterunner();
		    message->handled="true";
		 }
	         break;
	      case "chan.dtmf":
	         if(message->targetid == ourcallid)
		 {
                    logtmp->write("Notify:%O\n",message);
		    string text = message->params->text;
		    for( int i=0 ; i < sizeof(text) ; i++ )
		       staterunner(text[i]-48);
		    message->handled="true";
		 }
                 break;
           }
	   if(!message->ack)
	      yate->acknowledge(message);
	   break;
	case "answer":
	   break;
	case "installed":
	case "uninstalled":
	   yate->debug("Pike [Un]Installed: %s\n",message->name);
	   break;
	default:
	   yate->debug("Unknown type: %s\n",message->type);

      }

   }
}

void staterunner(void|int key)
{
   logtmp->write("state: %d\n",state);
   switch(state)
   {
     case WAIT:
        break;
     case RECSTART:
        play(VM_BASE+"/greeting.slin");
	break;
     case RECBEEP:
        play(VM_BASE+"/beep.slin");
	break;
     case RECREC:
        rec(VM_BASE+"/testrec.slin", 160000);
     case RECEND:
     case PLAYSTART:
        break;
     default:
        logtmp->write("This stae does not make sense %d\n",state);
   }
}

void play(string source)
{
   mapping message = (["type":"outgoing",
                      "name":"chan.attach",
                      "params":(["source":"wave/play/" + source,
		               "notify":ourcallid])]);
   yate->dispatch(message);
}

void rec(string source, int duration)
{
   mapping message = (["type":"outgoing",
                      "name":"chan.attach",
                      "params":(["consumer":"wave/record/" + source,
		                 "maxlen":duration,
		                 "notify":ourcallid])]);
   yate->dispatch(message);
}
