package com.pelleplutt.tuscedo;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.tuscedo.ui.UIWorkArea;

public class MNet extends MObj {
  public MNet(UIWorkArea wa, Compiler comp) {
    super(wa, comp, "net");
  }

  public void init(UIWorkArea wa, com.pelleplutt.operandi.Compiler comp) {
    this.workarea = wa;
    addFunc("ifc", OperandiScript.FN_NET_IFC, comp);
    addFunc("get", OperandiScript.FN_NET_GET, comp);
    addFunc("localhost", OperandiScript.FN_NET_LOCALHOST, comp);
  }
  
  public static void createNetFunctions(OperandiScript os) {
    os.setExtDef(OperandiScript.FN_NET_IFC, "() - returns a list of interfaces",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        MListMap listMap = new MListMap();
        Enumeration<NetworkInterface> e;
        try {
          e = NetworkInterface.getNetworkInterfaces();
          while (e.hasMoreElements()) {
            NetworkInterface nif = e.nextElement();
            listMap.add(new M(nif.getName()));
          }
          return new M(listMap);
        } catch (SocketException e1) {
        }
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_NET_LOCALHOST, "() - tries to make a vaild guess of localhost ip",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        try {
          return new M(getLocalHostLANAddress().getAddress());
        } catch (UnknownHostException e1) {
        }
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_NET_GET, "(<interface>) - returns a struct info of specific interface",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String name = null; 
        if (args.length > 0) {
          name = args[0].asString();
        }
        
        MListMap config = new MListMap();
        NetworkInterface nif = null;
        Enumeration<NetworkInterface> e;
        try {
          e = NetworkInterface.getNetworkInterfaces();
          while (e.hasMoreElements()) {
            NetworkInterface nife = e.nextElement();
            if (name == null || name.equals(nife.getName())) {
              nif = nife;
              break;
            };
          }
          
          if (nif == null) return null;
          config.put("display_name", new M(nif.getDisplayName()));
          config.put("name", new M(nif.getName()));
          config.put("hw_addr", new M(nif.getHardwareAddress()));
          MListMap addrs = new MListMap();
          for (InterfaceAddress ip : nif.getInterfaceAddresses()) {
            MListMap addr = new MListMap();

            addr.put("ip", new M(ip.getAddress().getAddress()));
            addr.put("network_prefix", new M(ip.getNetworkPrefixLength()));
            if (ip.getBroadcast() != null) {
              addr.put("broadcast_ip", new M(ip.getBroadcast().getAddress()));
            }
            addr.put("is_any_local", new M(ip.getAddress().isAnyLocalAddress() ? 1 : 0));
            addr.put("is_link_local", new M(ip.getAddress().isLinkLocalAddress() ? 1 : 0));
            addr.put("is_loopback", new M(ip.getAddress().isLoopbackAddress() ? 1 : 0));
            addr.put("is_multicast", new M(ip.getAddress().isMulticastAddress() ? 1 : 0));
            addr.put("is_site_local", new M(ip.getAddress().isSiteLocalAddress() ? 1 : 0));
            
            addrs.add(new M(addr));
          }
          config.put("addr", new M(addrs));
          
          return new M(config);
        } catch (SocketException e1) {
        }
        return null;
      }
    });
  }

  
  private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
    try {
      InetAddress candidateAddress = null;
      // Iterate all NICs (network interface cards)...
      for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
        NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
        // Iterate all IP addresses assigned to each card...
        for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
          InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
          if (!inetAddr.isLoopbackAddress()) {
            if (inetAddr.isSiteLocalAddress()) {
              return inetAddr;
            }
            else if (candidateAddress == null) {
              candidateAddress = inetAddr;
            }
          }
        }
      }
      if (candidateAddress != null) {
        return candidateAddress;
      }
      InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
      if (jdkSuppliedAddress == null) {
        throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
      }
      return jdkSuppliedAddress;
  }
  catch (Exception e) {
    UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
    unknownHostException.initCause(e);
    throw unknownHostException;
  }
}
}
