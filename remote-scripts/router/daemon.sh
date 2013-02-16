#!/bin/bash
REQKEY="** INSERT YOUR DREADSTATUS/DIMMEDIATESHUTDOWN KEY HERE **"
MACADDR="** INSERT YOUR MAC ADDRESS HERE **"
INTERF="br0"
REQ_URL="http://INSERTyourwebsite.com/dReadStatus.php?key=${REQKEY}"
IMM_OFF="http://INSERTyourwebsite.com/dImmediateShutdown.php?key=${REQKEY}"
while [ 1 ]; do
	RES="$(curl -s ${REQ_URL})"
	if [ "${RES}" == "0" ]; then
		curl http://INSERT_YOUR_INTERNAL_IP_ADDR:1337/do_shutdown
		curl "${IMM_OFF}"
	elif [ "${RES}" == "1" ]; then
		/usr/bin/ether-wake -i "${INTERF}" "${MACADDR}"	
	fi
	sleep 5
done
