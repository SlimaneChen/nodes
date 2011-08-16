/*
 * droid-client.c, Jan Repnak <io@jrx.de>, 03.2011
 *
 * coap-client -- simple CoAP client
 * Copyright (C) 2010 Olaf Bergmann <bergmann@tzi.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <ctype.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

#include <jni.h>
#include <android/log.h>

#include "coap.h"

/* Android LOGGING */
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "coap-client", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "coap-client", __VA_ARGS__))

/* Globale Variablen */
JavaVM* gJavaVM;
jobject thiz_global;
jmethodID aMethodIDJNIgetPacket;
jmethodID aMethodIDJNIgetNeighbor;
unsigned char *jnipayload="";

/** * **/

static coap_list_t *optlist = NULL;
/* Request URI.
 * TODO: associate the resources with transaction id and make it expireable */
static coap_uri_t uri;
static str proxy = { 0, NULL };

/* reading is done when this flag is set */
static int ready = 0;
static FILE *file = NULL;	/* output file name */

static str payload = { 0, NULL }; /* optional payload to send */

typedef unsigned char method_t;
method_t method = 1;		/* the method we are using in our requests */

extern unsigned int
print_readable( const unsigned char *data, unsigned int len,
		unsigned char *result, unsigned int buflen );

int
append_to_file(const char *filename, const unsigned char *data, size_t len) {
  size_t written;

  if ( !file && !(file = fopen(filename, "w")) ) {
    perror("append_to_file: fopen");
    return -1;
  }

  do {
    written = fwrite(data, 1, len, file);
    len -= written;
    data += written;
  } while ( written && len );

  return 0;
}

coap_pdu_t *
new_ack( coap_context_t  *ctx, coap_queue_t *node ) {
  coap_pdu_t *pdu = coap_new_pdu();

  if (pdu) {
    pdu->hdr->type = COAP_MESSAGE_ACK;
    pdu->hdr->code = 0;
    pdu->hdr->id = node->pdu->hdr->id;
  }

  return pdu;
}

coap_pdu_t *
new_response( coap_context_t  *ctx, coap_queue_t *node, unsigned int code ) {
  coap_pdu_t *pdu = new_ack(ctx, node);

  if (pdu)
    pdu->hdr->code = code;

  return pdu;
}

coap_pdu_t *
coap_new_request( method_t m, coap_list_t *options ) {
  coap_pdu_t *pdu;
  coap_list_t *opt;

  if ( ! ( pdu = coap_new_pdu() ) )
    return NULL;

  pdu->hdr->type = COAP_MESSAGE_CON;
  pdu->hdr->id = rand();	/* use a random transaction id */
  pdu->hdr->code = m;

  for (opt = options; opt; opt = opt->next) {
    coap_add_option( pdu, COAP_OPTION_KEY(*(coap_option *)opt->data),
		     COAP_OPTION_LENGTH(*(coap_option *)opt->data),
		     COAP_OPTION_DATA(*(coap_option *)opt->data) );
  }

  if (payload.length) {
    /* TODO: must handle block */

    coap_add_data(pdu, payload.length, payload.s);
  }

  return pdu;
}

int
send_request( coap_context_t  *ctx, coap_pdu_t  *pdu, const char *server, unsigned short port ) {
  struct addrinfo *res, *ainfo;
  struct addrinfo hints;
  int error;
  struct sockaddr_in6 dst;
  static unsigned char buf[COAP_MAX_PDU_SIZE]={0};
  memset ((char *)&hints, 0, sizeof(hints));
  hints.ai_socktype = SOCK_DGRAM;
  hints.ai_family = AF_INET6;

//  error = getaddrinfo(server, "", &hints, &res);
  error = getaddrinfo(server, NULL, &hints, &res);

  LOGI("server: %s\n", server);

  if (error != 0) {
    LOGI("getaddrinfo: %s\n", gai_strerror(error));
    return -2;
    //exit(1);
  }

  for (ainfo = res; ainfo != NULL; ainfo = ainfo->ai_next) {

    if ( ainfo->ai_family == AF_INET6 ) {

      memset(&dst, 0, sizeof dst );
      dst.sin6_family = AF_INET6;
      dst.sin6_port = htons( port );
      memcpy( &dst.sin6_addr, &((struct sockaddr_in6 *)ainfo->ai_addr)->sin6_addr, sizeof(dst.sin6_addr) );

//      strcpy ((unsigned char *) pdu->hdr,"");
/*
      strcpy(buf, "");
      LOGI("C - old data %s\n", buf);
      LOGI("C - pdu->hdr %s\n", pdu->hdr);
      LOGI("C - pdu->hdr->optcnt %d\n", pdu->hdr->optcnt);
      LOGI("C - pdu->hdr->type %d\n", pdu->hdr->type);

      LOGI("C - pdu->length %d\n", pdu->length);
      LOGI("C - pdu->options %s\n", pdu->options);
      LOGI("C - pdu->data %s\n", pdu->data);
*/

      print_readable( (unsigned char *)pdu->hdr, pdu->length, buf, COAP_MAX_PDU_SIZE);
      LOGI("C - send id %d\n",pdu->hdr->id);
      LOGI("C - send data %s\n",buf);
      coap_send_confirmed( ctx, &dst, pdu );
      //pdu->length=0;
      //strcpy (buf,"");
      //strcpy ((unsigned char *) pdu->hdr,"");

      goto leave;
    }
  }

 leave:
  freeaddrinfo(res);
  return 1;
}

#define COAP_OPT_BLOCK_LAST(opt) ( COAP_OPT_VALUE(*block) + (COAP_OPT_LENGTH(*block) - 1) )
#define COAP_OPT_BLOCK_MORE(opt) ( *COAP_OPT_LAST(*block) & 0x08 )
#define COAP_OPT_BLOCK_SIZE(opt) ( *COAP_OPT_LAST(*block) & 0x07 )

unsigned int
_read_blk_nr(coap_opt_t *opt) {
  unsigned int i, nr=0;
  for ( i = COAP_OPT_LENGTH(*opt); i; --i) {
    nr = (nr << 8) + COAP_OPT_VALUE(*opt)[i-1];
  }
  return nr >> 4;
}
#define COAP_OPT_BLOCK_NR(opt)   _read_blk_nr(&opt)

void
message_handler( coap_context_t  *ctx, coap_queue_t *node, void *data) {

  JNIEnv* env_global;
  (*gJavaVM)->GetEnv(gJavaVM, (void **) &env_global, JNI_VERSION_1_6);
  LOGI("message_handler env: %s", &env_global);

  coap_pdu_t *pdu = NULL;
  coap_opt_t *block, *ct, *sub, *coap_opt;
  unsigned int blocknr;
  unsigned char buf[4];
  coap_list_t *option;
  unsigned int len;
  unsigned char *databuf;


#ifndef NDEBUG
  LOGI("** process pdu: ");
  coap_show_pdu( node->pdu );
#endif

  if ( node->pdu->hdr->version != COAP_DEFAULT_VERSION ) {
    debug("dropped packet with unknown version %u\n", node->pdu->hdr->version);
    return;
  }

  if ( node->pdu->hdr->code < COAP_RESPONSE_100 && node->pdu->hdr->type == COAP_MESSAGE_CON ) {
    /* send 500 response */
    pdu = new_response( ctx, node, COAP_RESPONSE_500 );
    goto finish;
  }


  switch (node->pdu->hdr->code) {
  case COAP_RESPONSE_200:

	/* got some data, check if block option is set */
    block = coap_check_option( node->pdu, COAP_OPTION_BLOCK );
    if ( !block ) {
      /* There is no block option set, just read the data and we are done. */
      if ( coap_get_data( node->pdu, &len, &databuf ) ) {
	/*path = coap_check_option( node->pdu, COAP_OPTION_URI_PATH );*/
	//append_to_file( "coap.out", databuf, len );

       	 // LOGI("C - get data: %s", databuf);

       	  if (databuf) {

        	  jnipayload = (unsigned char *) calloc(strlen(databuf) + 1, sizeof(unsigned char));
        	  strcpy(jnipayload, databuf);

       	  }

//
//    	  //(*env_global)->CallVoidMethod(env_global,thiz_global,aMethodIDJNIgetString, (*env_global)->NewStringUTF(env_global, databuf));
//
//    	  (*env_global)->CallVoidMethod(
//    			  env_global,thiz_global,
//    			  aMethodIDJNIgetPacket,
//    			  node->pdu->hdr->id, 		//Message-ID
//    			  0,
//    			  node->pdu->hdr->code,		//Code
//    			  node->pdu->hdr->type,		//Type
//    			  node->pdu->hdr->optcnt,	//Option Count
//    			  (*env_global)->NewStringUTF(env_global, databuf)
//    			  );
      }
    } else {
      blocknr = coap_decode_var_bytes( COAP_OPT_VALUE(*block), COAP_OPT_LENGTH(*block) );

      /* TODO: check if we are looking at the correct block number */



      if ( coap_get_data( node->pdu, &len, &databuf ) ) {
	/*path = coap_check_option( node->pdu, COAP_OPTION_URI_PATH );*/
	//append_to_file( "coap.out", databuf, len );


//    	  LOGI("C - block nr: %d", blocknr);
//    	  LOGI("C - get data: %s", databuf);

       	  if (databuf) {
			  unsigned char *buf;
			  buf = (unsigned char *) calloc(strlen(jnipayload) + strlen(databuf) + 1, sizeof(unsigned char));
			  strcpy(buf, jnipayload);
			  strcat(buf, databuf);

			  jnipayload = (unsigned char *) calloc(strlen(buf) + 1, sizeof(unsigned char));
			  strcpy(jnipayload, buf);

			  free(buf);
       	  }

//    	  LOGI("C - get payload: %s", jnipayload);
//
//    	  (*env_global)->CallVoidMethod(
//    			  env_global,thiz_global,
//    			  aMethodIDJNIgetPacket,
//    			  node->pdu->hdr->id, 		//Message-ID
//    			  blocknr,
//    			  node->pdu->hdr->code,		//Code
//    			  node->pdu->hdr->type,		//Type
//    			  node->pdu->hdr->optcnt,	//Option Count
//    			  (*env_global)->NewStringUTF(env_global, databuf)
//    			  );
      }

      if ( (blocknr & 0x08) ) {
	/* more bit is set */
    	  LOGI("found the M bit, block size is %u, block nr. %u\n",
	       blocknr & 0x07,
	       (blocknr & 0xf0) << blocknr & 0x07);

	/* need to acknowledge if message was asyncronous */
	if ( node->pdu->hdr->type == COAP_MESSAGE_CON ) {
	  pdu = new_ack( ctx, node );

	  if ( pdu && coap_send( ctx, &node->remote, pdu ) == COAP_INVALID_TID ) {
	    debug("message_handler: error sending reponse");
	    coap_delete_pdu(pdu);
	    return;
	  }
	}

	/* create pdu with request for next block */
	pdu = coap_new_request( method, NULL ); /* first, create bare PDU w/o any option  */
	if ( pdu ) {
	  pdu->hdr->id = node->pdu->hdr->id; /* copy transaction id from response */

	  /* get content type from response */
	  ct = coap_check_option( node->pdu, COAP_OPTION_CONTENT_TYPE );
	  if ( ct ) {
	    coap_add_option( pdu, COAP_OPTION_CONTENT_TYPE,
			     COAP_OPT_LENGTH(*ct),COAP_OPT_VALUE(*ct) );
	  }

	  /* add URI components from optlist */
	  for (option = optlist; option; option = option->next ) {
	    switch (COAP_OPTION_KEY(*(coap_option *)option->data)) {
	    case COAP_OPTION_URI_AUTHORITY :
	    case COAP_OPTION_URI_PATH :
	    case COAP_OPTION_URI_QUERY :
	      coap_add_option ( pdu, COAP_OPTION_KEY(*(coap_option *)option->data),
				COAP_OPTION_LENGTH(*(coap_option *)option->data),
				COAP_OPTION_DATA(*(coap_option *)option->data) );
	      break;
	    default:
	      ;			/* skip other options */
	    }
	  }

	  /* finally add updated block option from response */
	  coap_add_option ( pdu, COAP_OPTION_BLOCK,
			    coap_encode_var_bytes(buf, blocknr + ( 1 << 4) ), buf);

	  if ( coap_send_confirmed( ctx, &node->remote, pdu ) == COAP_INVALID_TID ) {
	    debug("message_handler: error sending reponse");
	    coap_delete_pdu(pdu);
	  }
	  return;
	}
      }
    }

    break;
  default:
    ;
  }

	 /*
	 **  Describe Header + Options + Payload
	 */

	  LOGI("C - get id: %d", node->pdu->hdr->id);
	  LOGI("C - get code: %d", node->pdu->hdr->code);
	  LOGI("C - get type: %d", node->pdu->hdr->type);

	  // Check Option Content-Type
	  unsigned int optcontenttype = 0;
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_CONTENT_TYPE )) {
		  optcontenttype = coap_decode_var_bytes(COAP_OPT_VALUE(*coap_opt), COAP_OPT_LENGTH(*coap_opt));
		  LOGI("C - get content-type: %d", optcontenttype);
	  }

	  // Check Option Max-Age
	  unsigned int optmaxage = 0;
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_MAXAGE )) {
		  optmaxage = coap_decode_var_bytes(COAP_OPT_VALUE(*coap_opt), COAP_OPT_LENGTH(*coap_opt));
		  LOGI("C - get max-age: %d", optmaxage);
	  }

	  // Check Option URI_SCHEME
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_URI_SCHEME )) {
		  LOGI("C - found uri_scheme: ");
	  }
	  // Check Option E-TAG
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_ETAG )) {
		  LOGI("C - found e-tag: ");
	  }
	  // Check Option URI_AUTHORITY
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_URI_AUTHORITY )) {
		  LOGI("C - found uri authority: ");
	  }

	  // Check Option COAP_OPTION_LOCATION
	  jbyteArray jb_optlocationpath = NULL;
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_LOCATION )) {

		  	  jb_optlocationpath=(*env_global)->NewByteArray(env_global, COAP_OPT_LENGTH(*coap_opt));
	  	  	  (*env_global)->SetByteArrayRegion(env_global, jb_optlocationpath, 0, COAP_OPT_LENGTH(*coap_opt), (jbyte *) COAP_OPT_VALUE(*coap_opt));

		  LOGI("C - found location: ");
	  }
	  // Check Option COAP_OPTION_URI_PATH
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_URI_PATH  )) {
		  LOGI("C - found uri path: ");
	  }
	  // Check Option COAP_OPTION_TOKEN
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_TOKEN  )) {
		  LOGI("C - found token: ");
	  }
	  // Check Option COAP_OPTION_URI_QUERY
	  if (coap_opt = coap_check_option( node->pdu, COAP_OPTION_URI_QUERY  )) {
		  LOGI("C - found uri query: ");
	  }

	 /*
	 ** Send Header + Options + Payload to JNI
	 */

	  	  	  jbyteArray jb;
	  	  	  jb=(*env_global)->NewByteArray(env_global, strlen(jnipayload));
	  	  	  (*env_global)->SetByteArrayRegion(env_global, jb, 0, strlen(jnipayload), (jbyte *) jnipayload);

	      	  (*env_global)->CallVoidMethod(
	      			  env_global,thiz_global,
	      			  aMethodIDJNIgetPacket,
	      			  node->pdu->hdr->id, 		//Message-ID
	      			  node->pdu->hdr->code,		//Code
	      			  node->pdu->hdr->type,		//Type
	      			  node->pdu->hdr->optcnt,	//Option Count
	      			  optcontenttype,			//Option: ContentType
	      			  optmaxage,				//Option: MaxAge
	      			  jb_optlocationpath,		//Option: LocationPath
	      			  jb						//Payload
	      			  );

	      	  /* Release */

	    	  (*env_global)-> ReleaseByteArrayElements(env_global, jb, (jbyte *)jnipayload, 0);

	    	  jnipayload = (unsigned char *) calloc(strlen("") + 1, sizeof(unsigned char));
	    	  strcpy(jnipayload, "");


  /* acknowledge if requested */
  if ( !pdu && node->pdu->hdr->type == COAP_MESSAGE_CON ) {
    pdu = new_ack( ctx, node );
  }

  finish:
  if ( pdu && coap_send( ctx, &node->remote, pdu ) == COAP_INVALID_TID ) {
    debug("message_handler: error sending reponse");
    coap_delete_pdu(pdu);
  }

  /* our job is done, we can exit at any time */
  sub = coap_check_option( node->pdu, COAP_OPTION_SUBSCRIPTION );
  if ( sub ) {
    debug("message_handler: Subscription-Lifetime is %d\n",
	  COAP_PSEUDOFP_DECODE_8_4(*COAP_OPT_VALUE(*sub)));
  }
  ready = !sub || COAP_PSEUDOFP_DECODE_8_4(*COAP_OPT_VALUE(*sub)) == 0;
}

void
usage( const char *program, const char *version) {
  const char *p;

  p = strrchr( program, '/' );
  if ( p )
    program = ++p;

  LOGI("%s v%s -- a small CoAP implementation\n"
	   "(c) 2010 Olaf Bergmann <bergmann@tzi.org>\n\n"
	   "usage: %s [-b num] [-g group] [-m method] [-p port] [-s num] [-t type...] [-T string] URI\n\n"
	   "\tURI can be an absolute or relative coap URI,\n"
	   "\t-b size\t\tblock size to be used in GET/PUT/POST requests\n"
	   "\t       \t\t(value must be a multiple of 16 not larger than 2048)\n"
	   "\t-f file\t\tfile to send with PUT/POST (use '-' for STDIN)\n"
	   "\t-g group\tjoin the given multicast group\n"
	   "\t-m method\trequest method (get|put|post|delete)\n"
	   "\t-p port\t\tlisten on specified port\n"
	   "\t-s duration\tsubscribe for given duration [s]\n"
	   "\t-A types\taccepted content for GET (comma-separated list)\n"
	   "\t-t type\t\tcontent type for given resource for PUT/POST\n"
	   "\t-P addr[:port]\tuse proxy (automatically adds Uri-Authority option to request)\n"
	   "\t-T token\tinclude specified token\n",
	   program, version, program );
}

int
join( coap_context_t *ctx, char *group_name ){
  struct ipv6_mreq mreq;
  struct addrinfo   *reslocal = NULL, *resmulti = NULL, hints, *ainfo;
  int result = -1;

  /* we have to resolve the link-local interface to get the interface id */
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_INET6;
  hints.ai_socktype = SOCK_DGRAM;

  result = getaddrinfo("::", NULL, &hints, &reslocal);
  if ( result < 0 ) {
	  LOGI("join: cannot resolve link-local interface: %s\n",
	    gai_strerror(result));
    goto finish;
  }

  /* get the first suitable interface identifier */
  for (ainfo = reslocal; ainfo != NULL; ainfo = ainfo->ai_next) {
    if ( ainfo->ai_family == AF_INET6 ) {
      mreq.ipv6mr_interface =
	      ((struct sockaddr_in6 *)ainfo->ai_addr)->sin6_scope_id;
      break;
    }
  }

  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_INET6;
  hints.ai_socktype = SOCK_DGRAM;

  /* resolve the multicast group address */
  result = getaddrinfo(group_name, NULL, &hints, &resmulti);

  if ( result < 0 ) {
	  LOGI("join: cannot resolve multicast address: %s\n",
	    gai_strerror(result));
    goto finish;
  }

  for (ainfo = resmulti; ainfo != NULL; ainfo = ainfo->ai_next) {
    if ( ainfo->ai_family == AF_INET6 ) {
      mreq.ipv6mr_multiaddr =
	((struct sockaddr_in6 *)ainfo->ai_addr)->sin6_addr;
      break;
    }
  }

  result = setsockopt( ctx->sockfd, IPPROTO_IPV6, IPV6_JOIN_GROUP,
		       (char *)&mreq, sizeof(mreq) );
  if ( result < 0 )
    perror("join: setsockopt");

 finish:
  freeaddrinfo(resmulti);
  freeaddrinfo(reslocal);

  return result;
}

int
order_opts(void *a, void *b) {
  if (!a || !b)
    return a < b ? -1 : 1;

  if (COAP_OPTION_KEY(*(coap_option *)a) < COAP_OPTION_KEY(*(coap_option *)b))
    return -1;

  return COAP_OPTION_KEY(*(coap_option *)a) == COAP_OPTION_KEY(*(coap_option *)b);
}


coap_list_t *
new_option_node(unsigned short key, unsigned int length, unsigned char *data) {
  coap_option *option;
  coap_list_t *node;

  option = coap_malloc(sizeof(coap_option) + length);
  if ( !option )
    goto error;

  COAP_OPTION_KEY(*option) = key;
  COAP_OPTION_LENGTH(*option) = length;
  memcpy(COAP_OPTION_DATA(*option), data, length);

  /* we can pass NULL here as delete function since option is released automatically  */
  node = coap_new_listnode(option, NULL);

  if ( node )
    return node;

 error:
  perror("new_option_node: malloc");
  coap_free( option );
  return NULL;
}

void
cmdline_content_type(char *arg, unsigned short key) {
  static char *content_types[] =
    { "plain", "xml", "csv", "html", "","","","","","","","","","","","","","","","","",
      "gif", "jpeg", "png", "tiff", "audio", "video", "","","","","","","","","","","","","",
      "link", "axml", "binary", "rdf", "soap", "atom", "xmpp", "exi",
      "bxml", "infoset", "json", 0};
  coap_list_t *node;
  unsigned char i, value[10];
  int valcnt = 0;
  char *p, *q = arg;

  while (q && *q) {
    p = strchr(q, ',');

    for (i=0; content_types[i] &&
	   strncmp(q,content_types[i], p ? p-q : strlen(q)) != 0 ;
	 ++i)
      ;

    if (content_types[i]) {
      value[valcnt] = i;
      valcnt++;
    } else {
    	LOGI("W: unknown content-type '%s'\n",arg);
    }

    if (!p || key == COAP_OPTION_CONTENT_TYPE)
      break;

    q = p+1;
  }

  if (valcnt) {
    node = new_option_node(key, valcnt, value);
    if (node)
      coap_insert( &optlist, node, order_opts );
  }
}

void
cmdline_uri(char *arg) {


coap_split_uri((unsigned char *)arg, &uri );

//LOGI("C - arg %s", arg);
//LOGI("C - uri.na %s", uri.na.s);
//LOGI("C - uri.na %d", uri.na.length);
//LOGI("C - uri.path %s", uri.path.s);
//LOGI("C - uri.path %d", uri.path.length);
//LOGI("C - uri.query %s", uri.query.s);
//LOGI("C - uri.query %d", uri.query.length);



#if 0  /* need authority only for proxy requests */
  if (uri.na.length)
    coap_insert( &optlist, new_option_node(COAP_OPTION_URI_AUTHORITY,
					   uri.na.length, uri.na.s),
		 order_opts);
#endif
  if (uri.path.length)
    coap_insert( &optlist, new_option_node(COAP_OPTION_URI_PATH,
					   uri.path.length, uri.path.s),
					   order_opts);

  if (uri.query.length)
    coap_insert( &optlist, new_option_node(COAP_OPTION_URI_QUERY,
					   uri.query.length, uri.query.s),
					   order_opts);

//  LOGI("C - optlist->data %d", optlist->data);
}

void
cmdline_blocksize(char *arg) {
  static unsigned char buf[4];	/* hack: temporarily take encoded bytes */
  unsigned int blocksize = atoi(arg);

  if ( COAP_MAX_PDU_SIZE < blocksize + sizeof(coap_hdr_t) ) {
	  LOGI("W: skipped invalid blocksize\n");
    return;
  }


  /* use only last three bits and clear M-bit */
  blocksize = (coap_fls(blocksize >> 4) - 1) & 0x07;
  coap_insert( &optlist, new_option_node(COAP_OPTION_BLOCK,
					 coap_encode_var_bytes(buf, blocksize), buf),
					 order_opts);
}

void
cmdline_subscribe(char *arg) {
  unsigned int ls, s;
  unsigned char duration = COAP_PSEUDOFP_ENCODE_8_4_UP(atoi(arg), ls, s);

  coap_insert( &optlist, new_option_node(COAP_OPTION_SUBSCRIPTION,
					 1, &duration), order_opts );
}

void
cmdline_proxy(char *arg) {
  proxy.length = strlen(arg);
  if ( (proxy.s = coap_malloc(proxy.length + 1)) == NULL) {
    proxy.length = 0;
    return;
  }

  memcpy(proxy.s, arg, proxy.length+1);
}

void
cmdline_token(char *arg) {
  coap_insert( &optlist, new_option_node(COAP_OPTION_TOKEN,
					 strlen(arg),
					 (unsigned char *)arg), order_opts);
}

int
cmdline_input_from_file(char *filename, str *buf) {
  FILE *inputfile = NULL;
  ssize_t len;
  int result = 1;
  struct stat statbuf;

  if (!filename || !buf)
    return 0;

  if (filename[0] == '-' && !filename[1]) { /* read from stdin */
    buf->length = 20000;
    buf->s = (unsigned char *)coap_malloc(buf->length);
    if (!buf->s)
      return 0;

    inputfile = stdin;
  } else {
    /* read from specified input file */
    if (stat(filename, &statbuf) < 0) {
      perror("cmdline_input_from_file: stat");
      return 0;
    }

    buf->length = statbuf.st_size;
    buf->s = (unsigned char *)coap_malloc(buf->length);
    if (!buf->s)
      return 0;

    inputfile = fopen(filename, "r");
    if ( !inputfile ) {
      perror("cmdline_input_from_file: fopen");
      coap_free(buf->s);
      return 0;
    }
  }

  len = fread(buf->s, 1, buf->length, inputfile);

  if (len < buf->length) {
    if (ferror(inputfile) != 0) {
      perror("cmdline_input_from_file: fread");
      coap_free(buf->s);
      buf->length = 0;
      buf->s = NULL;
      result = 0;
    } else {
      buf->length = len;
    }
  }

  if (inputfile != stdin)
    fclose(inputfile);

  return result;
}

method_t
cmdline_method(char *arg) {
  static char *methods[] =
    { 0, "get", "post", "put", "delete", 0};
  unsigned char i;

  for (i=1; methods[i] && strcasecmp(arg,methods[i]) != 0 ; ++i)
    ;

  return i;	     /* note that we do not prevent illegal methods */
}

coap_context_t *
get_context(const char *node, const char *port) {
  coap_context_t *ctx = NULL;
  int s;
  struct addrinfo hints;
  struct addrinfo *result, *rp;

  memset(&hints, 0, sizeof(struct addrinfo));
  hints.ai_family = AF_UNSPEC;    /* Allow IPv4 or IPv6 */
  hints.ai_socktype = SOCK_DGRAM; /* Coap uses UDP */
//  hints.ai_flags = AI_PASSIVE | AI_NUMERICHOST | AI_NUMERICSERV | AI_ALL;
  hints.ai_flags = AI_PASSIVE | AI_NUMERICHOST | AI_NUMERICSERV;

  s = getaddrinfo(node, port, &hints, &result);
  if ( s != 0 ) {
	  LOGI("getaddrinfo: %s\n", gai_strerror(s));
    return NULL;
  }

  /* iterate through results until success */
  for (rp = result; rp != NULL; rp = rp->ai_next) {
    ctx = coap_new_context(rp->ai_addr, rp->ai_addrlen);
    if (ctx) {
      /* TODO: output address:port for successful binding */
      goto finish;
    }
  }

  LOGI("no context available for interface '%s'\n", node);

 finish:
  freeaddrinfo(result);
  return ctx;
}

/** Node Discovery **/

/** * **/

int
_order_transaction_id( coap_queue_t *lhs, coap_queue_t *rhs );

#define options_start(p) ((coap_opt_t *) ( (unsigned char *)p->hdr + sizeof ( coap_hdr_t ) ))

#define options_end(p, opt) {			\
  unsigned char opt_code = 0, cnt;		\
  *opt = options_start( node->pdu );            \
  for ( cnt = (p)->hdr->optcnt; cnt; --cnt ) {  \
    opt_code += COAP_OPT_DELTA(**opt);			\
    *opt = (coap_opt_t *)( (unsigned char *)(*opt) + COAP_OPT_SIZE(**opt)); \
  } \
}

int
coap_read_nd( coap_context_t *ctx ) {
  static char buf[COAP_MAX_PDU_SIZE];
  static coap_hdr_t *pdu = (coap_hdr_t *)buf;
  ssize_t bytes_read;
  static struct sockaddr_in6 src;
  socklen_t addrsize = sizeof src;
  coap_queue_t *node;
  coap_opt_t *opt;
  int i=0;

  static char addr[INET6_ADDRSTRLEN];

  bytes_read = recvfrom( ctx->sockfd, buf, COAP_MAX_PDU_SIZE, 0,
			 (struct sockaddr *)&src, &addrsize );

  if ( bytes_read < 0 ) {
    LOGW("coap_read: recvfrom");
    return -1;
  }

  if ( bytes_read < sizeof(coap_hdr_t) ) {
    LOGI("coap_read: discarded invalid frame\n" );
    return -1;
  }

  if ( pdu->version != COAP_DEFAULT_VERSION ) {
    LOGI("coap_read: unknown protocol version\n" );
    return -1;
  }

  node = coap_new_node();
  if ( !node )
    return -1;

  node->pdu = coap_new_pdu();
  if ( !node->pdu ) {
    coap_delete_node( node );
    return -1;
  }

  time( &node->t );
  memcpy( &node->remote, &src, sizeof( src ) );

  /* "parse" received PDU by filling pdu structure */
  memcpy( node->pdu->hdr, buf, bytes_read );
  node->pdu->length = bytes_read;

  /* finally calculate beginning of data block */
  options_end( node->pdu, &opt );

  if ( (unsigned char *)node->pdu->hdr + node->pdu->length < (unsigned char *)opt )
    node->pdu->data = (unsigned char *)node->pdu->hdr + node->pdu->length;
  else
    node->pdu->data = (unsigned char *)opt;

  /* and add new node to receive queue */
  coap_insert_node( &ctx->recvqueue, node, _order_transaction_id );
  if ( inet_ntop(src.sin6_family, &src.sin6_addr, addr, INET6_ADDRSTRLEN) == 0 ) {
    LOGW("coap_read: inet_ntop");
  } else {

	  JNIEnv* env_global;
	  (*gJavaVM)->GetEnv(gJavaVM, (void **) &env_global, JNI_VERSION_1_6);
	  LOGI("message_handler env: %s", &env_global);

  	  (*env_global)->CallVoidMethod(
  			  env_global,thiz_global,
  			  aMethodIDJNIgetNeighbor,
  			  (*env_global)->NewStringUTF(env_global, addr), //addr
  			  ntohs(src.sin6_port) //port
  			  );

    LOGI("** received from [%s]:%d:\n  ",addr,ntohs(src.sin6_port));
  }
  coap_show_pdu( node->pdu );

  return 0;
}

/** * **/

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	gJavaVM = vm;
	LOGI("!!! JNI_OnLoad called");
	return JNI_VERSION_1_4;
}

/** * **/

jint Java_de_jrx_ad_nodes_service_DiscoveryRequestHandler_getNeighbors
	(JNIEnv* env, jobject thiz, jint jargc, jstring jargv, jbyteArray jpayload)
{
	(*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL);
	thiz_global  = (*env)->NewGlobalRef(env, thiz);

	return CoapClient(env, thiz, jargc, jargv, jpayload, 1); //Set discovery_flag
}

/** * **/

jint Java_de_jrx_ad_nodes_service_CoapRequestHandler_controlClient
	(JNIEnv* env, jobject thiz, jint jargc, jstring jargv, jbyteArray jpayload)
{
	(*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL);
	thiz_global  = (*env)->NewGlobalRef(env, thiz);

	return CoapClient(env, thiz, jargc, jargv, jpayload, 0);
}

/** * **/

int CoapClient (JNIEnv* env, jobject thiz, jint jargc, jstring jargv, jbyteArray jpayload, int discovery_flag) {

	int coap_send_id = 0;
	int loop;
	int argc=jargc;
	char *argv[50];
	char argv_string[500]=""; /* @TODO Good Length of URI*/

	for(loop=0;loop<50;loop++)
	{
		argv[loop]==NULL;
	}


	//Covert JString
	strcpy (argv_string,(*env)->GetStringUTFChars(env, jargv, 0));

	LOGI("uebergeben: %s", argv_string);

	//Split argv_string to a Array
	argv[0]=strtok(argv_string," ");
	for(loop=1;loop<argc;loop++)
	{
		argv[loop]=strtok(NULL," ");
		if(argv[loop]==NULL)
			break;
	}

//	LOGI("argc %d", argc);
//	LOGI("argv[0] %s", argv[0]);
//	LOGI("argv[1] %s", argv[1]);
//	LOGI("argv[2] %s", argv[2]);
//	LOGI("argv[3] %s", argv[3]);
//
//	LOGI("argv %s", &argv);

	//sleep(5);
	/** DEBUG **/

	JNIEnv* env_test;
	(*gJavaVM)->GetEnv(gJavaVM, (void **) &env_test, JNI_VERSION_1_6);

	LOGI("env: %s || %s:", &env, &env_test );

	/** * **/

	jclass cls = (*env)->GetObjectClass(env,thiz);

	/* Assign java functions */
	aMethodIDJNIgetPacket = (*env)->GetMethodID(env, cls, "JNIgetPacket", "(IIIIII[B[B)V");
	jmethodID aMethodIDJNIisRunning = (*env)->GetMethodID(env, cls, "JNIisRunning", "()I");

	if (discovery_flag == 1)
		aMethodIDJNIgetNeighbor = (*env)->GetMethodID(env, cls, "JNIgetNeighbor", "(Ljava/lang/String;I)V");



	/** BUGFIX **/

		optlist = 0;

		// Initialize getopt
		optind=0;
		opterr=0;
	    optopt=0;

	/** ****** **/

	/** BEGIN CoAP-Client **/

	  coap_context_t  *ctx;
	  fd_set readfds;
	  struct timeval tv, *timeout;
	  int result;
	  time_t now;
	  coap_queue_t *nextpdu;
	  coap_pdu_t  *pdu;
	  static unsigned char *p;
	  static str server;
	  unsigned short port = COAP_DEFAULT_PORT;
	  char port_str[NI_MAXSERV] = "0";
	  int opt=0;
	  char *group = NULL;

	  //coap_free_context( ctx );

	  while ((opt = getopt(argc, argv, "b:f:g:m:p:s:t:A:P:T:")) != -1) {

//	      LOGI ("C - optarg: %d", opt);
//	      LOGI ("C - optarg: %s", optarg);

	    switch (opt) {
	    case 'b' :
	      cmdline_blocksize(optarg);
	      break;
	    case 'f' :

	    	if (optarg[0] == '-' && !optarg[1]) {


	    			payload.length = (*env)->GetArrayLength(env, jpayload);
		    		payload.s = (unsigned char *)coap_malloc(payload.length+1);
		    		strcpy(payload.s, (*env)->GetByteArrayElements(env, jpayload, 0));

		    		//(*env)-> ReleaseByteArrayElements(env, jpayload, (jbyte *)payload.s, 0);

		    		LOGI ("C: - jpayload: %s length: %d", payload.s, payload.length);

	    	} else {
	    		cmdline_input_from_file(optarg,&payload);
	    	}
	      break;
	    case 'g' :
	      group = optarg;
	      break;
	    case 'p' :
	      strncpy(port_str, optarg, NI_MAXSERV-1);
	      port_str[NI_MAXSERV - 1] = '\0';
	      break;
	    case 'm' :
	      method = cmdline_method(optarg);
	      break;
	    case 's' :
	      cmdline_subscribe(optarg);
	      break;
	    case 'A' :
	      cmdline_content_type(optarg,COAP_OPTION_ACCEPT);
	      break;
	    case 't' :
	      cmdline_content_type(optarg,COAP_OPTION_CONTENT_TYPE);
	      break;
	    case 'P' :
	      cmdline_proxy(optarg);
	      break;
	    case 'T' :
	      cmdline_token(optarg);
	      break;
	    default:
	      usage( argv[0], PACKAGE_VERSION );
	      exit( 1 );
	    }
	  }


	  /** * **/

	  ctx = get_context("::", port_str);
	  if ( !ctx )
	    return -1;

	  coap_register_message_handler( ctx, message_handler );

	  if ( optind < argc )
	    cmdline_uri( argv[optind] );
	  else {
	    usage( argv[0], PACKAGE_VERSION );
	    exit( 1 );
	  }

	  if ( group )
	    join( ctx, group );

	  /* include authority if proxy is used */
	  if (proxy.length) {
	    server = proxy;
	    if (uri.na.length)
	      coap_insert( &optlist, new_option_node(COAP_OPTION_URI_AUTHORITY,
						     uri.na.length, uri.na.s),
			   order_opts);
	  } else
	    server = uri.na;


	  if (! (pdu = coap_new_request( method, optlist ) ) )
	    return -1;

	  /* split server address and port */
	  if (server.length) {
	    if (*server.s == '[') {	/* IPv6 address reference */
	      p = ++server.s;
	      --server.length;

	      while ( p - server.s < server.length && *p != ']' )
		++p;

	      if (*p == ']')
		*p++ = '\0';		/* port starts here */
	    } else {			/* IPv4 address or hostname */
	      p = server.s;
	      while ( p - server.s < server.length && *p != ':' )
		++p;
	    }

	    if (*p == ':') {		/* port starts here */
	      *p++ = '\0';
	      port = 0;

	      /* set port */
	      while( p - server.s < server.length && isdigit(*p) ) {
		port = port * 10 + ( *p - '0' );
		++p;
	      }
	    }
	  }

	  /* send request */
	  if (send_request( ctx, pdu, server.length ? (char *)server.s : "::1", port ) == -2) {
		  return -2;
	  }
      coap_send_id = pdu->hdr->id;

	  while ( !(ready && coap_can_exit(ctx)) ) {

		/* Terminating the thread */

		  if (!(*env)->CallIntMethod(env,thiz,aMethodIDJNIisRunning)) {
			    LOGI("C - Thread Stopped");
			    return -3;
		  }

		/* ** */

	    FD_ZERO(&readfds);
	    FD_SET( ctx->sockfd, &readfds );

	    nextpdu = coap_peek_next( ctx );

	    time(&now);
	    while ( nextpdu && nextpdu->t <= now ) {
	      coap_retransmit( ctx, coap_pop_next( ctx ) );
	      nextpdu = coap_peek_next( ctx );
	    }

	    if ( nextpdu ) {	        /* set timeout if there is a pdu to send */
	      tv.tv_usec = 0;
	      tv.tv_sec = nextpdu->t - now;
	      timeout = &tv;
	    } else
	    	timeout = NULL;		/* no timeout otherwise */

	    result = select( ctx->sockfd + 1, &readfds, 0, 0, timeout );

	    if ( result < 0 ) {		/* error */
	    	  LOGI("C - select error");
	    } else if ( result > 0 ) {	/* read from socket */
	      if ( FD_ISSET( ctx->sockfd, &readfds ) ) {

	    	  if (discovery_flag == 0) {
	    		  coap_read( ctx );	/* read received data */
	    	  } else {
	    		  /* Node Discovery */
				  coap_read_nd( ctx );
	    	  }

	    	  coap_dispatch( ctx );	/* and dispatch PDUs from receivequeue */

	      }
	    }
	  }

	  if ( file ) {
	    fflush( file );
	    fclose( file );
	  }

	  // Release
	  coap_free_context( ctx );
	  (*env)->DeleteGlobalRef(env, thiz_global);

	  /** END CoAP-Client **/


	  return coap_send_id;

}

