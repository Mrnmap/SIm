#!/bin/sh
### BEGIN INIT INFO
# Provides:          l1fwd
# Required-Start:    
# Required-Stop:     $local_fs
# Default-Start:     5
# Default-Stop:      0 6
# Short-Description: Start screen session with l1fwd software
# Description:       
### END INIT INFO

. /etc/default/rcS

case "$1" in
        start)
		/usr/bin/screen -d -m -c /etc/osmocom/screenrc-l1fwd
                ;;
	stop)
		echo "This script doesn't support stop"
                exit 1
		;;
        restart|reload|force-reload)
                exit 0
                ;;
	show)
		;;
        *)
                echo "Usage: sysmobts {start|stop|show|reload|restart}" >&2
                exit 1
                ;;
esac
                                                                                                                                                                                                                                                                                                                                                   ",
                                     "params":(["targetid":partycallid,
                                                  "id":ourcallid])]);
                  yate->dispatch(answer);
                  answer = (["type":"outgoing",
                             "name":"chan.attach",
                             "params":([
                             "source":"fax/receive"+FAXBASE+get_filename(),
                             "notify":ourcallid])]);
/*
                  answer = (["type":"outgoing",
                      "name":"chan.attach",
                      "params":(["source":"wave/play//var/spool/voicemail/greeting.slin",
                               "notify":ourcallid ])]);
*/
                  yate->dispatch(answer);
                  break;
               case "chan.notify":
                  if(message->targetid == ourcallid)
                  {
                     //state=0;
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

string get_filename()
{
  int filename=0;
  Stdio.File seqf = Stdio.File(FAXBASE+"/seqf","wrc");
  seqf->lock();
  filename = (int) seqf->read();
  seqf->truncate(0);
  seqf->seek(0);
  seqf->write("%d\n",++filename);
  return sprintf("/%d.tiff",filename);
}
