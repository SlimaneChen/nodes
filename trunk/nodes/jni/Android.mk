LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := coap-client
LOCAL_SRC_FILES := droid-client.c debug.c encode.c list.c net.c pdu.c str.c subscribe.c uri.c
LOCAL_LDLIBS    := -llog 

include $(BUILD_SHARED_LIBRARY)

