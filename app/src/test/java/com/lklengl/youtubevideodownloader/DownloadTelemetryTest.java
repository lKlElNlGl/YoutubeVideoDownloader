package com.lklengl.youtubevideodownloader;

import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadTelemetryTest {
 @Test public void extractsProgressBytesAndSpeed(){
  String line="[download]  62.0% of 1.90GiB at 11.4MiB/s ETA 00:12";
  String out=DownloadTelemetry.summary(line,62);
  assertTrue(out.contains("из"));
  assertTrue(out.contains("МБ/с"));
 }
 @Test public void neverInventsSpeedWhenUnknown(){
  assertEquals("",DownloadTelemetry.summary("extracting information",20));
 }
}
