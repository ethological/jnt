package war.jnt.utility;

import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Memory monitoring utility to help track memory usage during transpilation
 */
public class MemoryMonitor {
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final long MEGABYTE = 1024 * 1024;
    
    public static void logMemoryUsage(String phase) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMB = heapUsage.getUsed() / MEGABYTE;
        long maxMB = heapUsage.getMax() / MEGABYTE;
        long committedMB = heapUsage.getCommitted() / MEGABYTE;

        Logger.INSTANCE.log(Level.MEMORY, Origin.CORE, String.format("%s - Used: %dMB, Committed: %dMB, Max: %dMB (%.1f%%)\n",
            phase, usedMB, committedMB, maxMB, (double) usedMB / maxMB * 100));

    }
    
    public static boolean isMemoryPressureHigh() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double usageRatio = (double) heapUsage.getUsed() / heapUsage.getMax();
        return usageRatio > 0.8; // 80% threshold
    }
    
    public static void forceGCIfNeeded() {
        if (isMemoryPressureHigh()) {
            System.gc();
            Logger.INSTANCE.log(Level.MEMORY, Origin.CORE, "High memory pressure detected, forcing GC\n");
        }
    }
    
    public static long getUsedMemoryMB() {
        return memoryBean.getHeapMemoryUsage().getUsed() / MEGABYTE;
    }
    
    public static long getMaxMemoryMB() {
        return memoryBean.getHeapMemoryUsage().getMax() / MEGABYTE;
    }
}

