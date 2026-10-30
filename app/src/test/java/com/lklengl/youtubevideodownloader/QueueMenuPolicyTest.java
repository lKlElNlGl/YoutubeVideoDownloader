package com.lklengl.youtubevideodownloader;
import org.junit.Test;
import static org.junit.Assert.*;
public class QueueMenuPolicyTest {
 @Test public void singleDangerousItemHasNoSeparator(){
  assertFalse(QueueMenuPolicy.needsSeparator(1,0,true));
 }
 @Test public void dangerLastWithEarlierActionsHasSeparator(){
  assertTrue(QueueMenuPolicy.needsSeparator(4,3,true));
 }
 @Test public void otherItemsHaveNoDangerDivider(){
  assertFalse(QueueMenuPolicy.needsSeparator(4,0,false));
 }
}