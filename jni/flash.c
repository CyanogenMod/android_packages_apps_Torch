


#include <fcntl.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <jni.h>
#include <utils/Log.h>

int dev;
int led_mode;
int ioctlRetVal = 1;

JNIEXPORT jstring JNICALL Java_net_cactii_flash2_FlashDevice_openFlash(JNIEnv* env, jclass class, jstring deviceObj)
{
  const char *device = (*env)->GetStringUTFChars(env, deviceObj, 0);
  dev = open(device, O_RDWR);
  jstring ret = (*env)->NewStringUTF(env, dev < 0 ? "Failed" : "OK");
  LOGD("open %s fd=%d", device, dev);
  (*env)->ReleaseStringUTFChars(env, deviceObj, device);
  return ret;
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash2_FlashDevice_flashWritable(JNIEnv* env, jclass class, jstring deviceObj)
{
  const char *device = NULL;
  if (deviceObj == NULL) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  device = (*env)->GetStringUTFChars(env, deviceObj, 0);
  LOGI("stat: %s", device);
  struct stat st;
  jstring ret = (*env)->NewStringUTF(env, stat(device, &st) < 0 ? "Failed open" : "OK");
  (*env)->ReleaseStringUTFChars(env, deviceObj, device);
  return ret;
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash2_FlashDevice_setFlashOff(JNIEnv *env)
{

  led_mode = 0;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash2_FlashDevice_setFlashOn(JNIEnv *env)
{

  led_mode = 1;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash2_FlashDevice_setFlashFlash(JNIEnv *env)
{

  led_mode = 4;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash2_FlashDevice_closeFlash(JNIEnv *env)
{
  if (dev > 0) {
    close(dev);
    return (*env)->NewStringUTF(env, "OK");
  }
  return (*env)->NewStringUTF(env, "Failed");
}

