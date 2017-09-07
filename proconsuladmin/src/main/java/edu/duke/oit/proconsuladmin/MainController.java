package edu.duke.oit.proconsuladmin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles requests for the application home page.
 */
@Controller
public class MainController {
	
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);
	
	// Authorization (minimal at first)
	private boolean isAdmin(HttpServletRequest request) {
		// for now, get the list from the config file
		PCAdminConfig config = PCAdminConfig.getInstance();
		String alist = config.getProperty("pcadminlist", true);
		String[] aa = alist.split(",");
		for (String u : aa) {
			if (request.getRemoteUser().equalsIgnoreCase(u)) {
				return true;
			}
		}
		return false;
	}
	
	@RequestMapping(value="/host_and_gateway_details", method=RequestMethod.POST)
	public ModelAndView handlePostOfHostAndGatewayDetails(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("redirect:https://" + request.getLocalName()+"/admin/host_and_gateway_details");
		
		PCAdminConfig config = PCAdminConfig.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed connecting to database: " + e.getMessage());
			}
			if (conn == null) {
				throw new RuntimeException("Failed getting database connection");
			}
			
			if (request.getParameter("groupformsubmitted") != null && request.getParameter("groupformsubmitted").equals("1")) {
				// this is a delete post in the proconsul host groups list
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^delhgrp[0-9]*") && request.getParameter(t).equals("1")) {
						// deleting a host to group mapping
						String num = t.replace("delhgrp","");
						String hname = request.getParameter("hostfqdn"+num);
						String gname = request.getParameter("hgrp"+num);
						ps = conn.prepareStatement("delete from host_access_group where fqdn = ? and groupdn = ?");
						ps.setString(1, hname);
						ps.setString(2,gname);
						ps.executeUpdate();
					} else if (t.matches("^delogrp[0-9]*") && request.getParameter(t).equals("1")) {
						// deleting an ou group mapping
						String num = t.replace("delogrp", "");
						String hname = request.getParameter("oudn"+num);
						String gname = request.getParameter("ogrp"+num);
						ps = conn.prepareStatement("delete from host_access_group where ou = ? and groupdn = ?");
						ps.setString(1,  hname);
						ps.setString(2, gname);
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("gwformsubmitted") != null && request.getParameter("gwformsubmitted").equals("1")) {
				// delete of a gw entry
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^delgw[0-9]*") && request.getParameter(t).equals("1")) {
						// deleting a gw mapping
						String num = t.replace("delgw", "");
						String lval = request.getParameter("tfqdn"+num);
						String mval = request.getParameter("hgw"+num);
						String rval = request.getParameter("hgwg"+num);
						ps = conn.prepareStatement("delete from host_gateway where fqdn=? and gateway=? and groupdn=?");
						ps.setString(1, lval);
						ps.setString(2, mval);
						ps.setString(3, rval);
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("addhostmapformsubmitted") != null && request.getParameter("addhostmapformsubmitted").equals("1")) {
				// adding a host group mapping
				if (request.getParameter("lhsselector").equals("fqdn")) {
					// mapping from host fqdn
					ps = conn.prepareStatement("insert into host_access_group values(?,?,?)");
					ps.setString(1,request.getParameter("lhs"));
					ps.setString(2,null);
					ps.setString(3,request.getParameter("rhs"));
					ps.executeUpdate();
				} else if (request.getParameter("lhsselector").equals("ou")) {
					// mapping from ou
					ps = conn.prepareStatement("insert into host_access_group values(?,?,?)");
					ps.setString(1,null);
					ps.setString(2, request.getParameter("lhs"));
					ps.setString(3,request.getParameter("rhs"));
					ps.executeUpdate();
				}
			} else if (request.getParameter("addgwformsubmitted") != null && request.getParameter("addgwformsubmitted").equals("1")) {
				// adding an ou mapping
				ps = conn.prepareStatement("insert into host_gateway values(?,?,?)");
				ps.setString(1, request.getParameter("lhs"));
				ps.setString(2, request.getParameter("rhs"));
				ps.setString(3, request.getParameter("other"));
				ps.executeUpdate();
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Failed updating database: " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
			
		return retval;
	}
	
	@RequestMapping(value="/windows_users", method=RequestMethod.POST)
	public ModelAndView handlePostOfWindowsUsers(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("redirect:https://" + request.getLocalName()+"/admin/windows_users");
		
		PCAdminConfig config = PCAdminConfig.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed connecting to database: " + e.getMessage());
			}
			if (conn == null) {
				throw new RuntimeException("Failed getting database connection");
			}
			if (request.getParameter("aformsubmitted") != null && request.getParameter("aformsubmitted").equals("1")) {
				// Delete -- find and process the apporpriate removal
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^delugrp[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete of a user mapping
						String num = t.replace("delugrp", "");
						String hname = request.getParameter("ugrp"+num);
						String uname = request.getParameter("uname"+num);
						// delete the entry from the database
						ps = conn.prepareStatement("delete from explicit_groups where eppn = ? and groupdn = ?");
						ps.setString(1, uname);
						ps.setString(2, hname);
						ps.executeUpdate();
					} else if (t.matches("^delggrp[0-9]*") && request.getParameter(t).equals("1")) {
						// delete of a group mapping
						String num = t.replace("delggrp","");
						String hname = request.getParameter("ggrp"+num);
						String uname = request.getParameter("gname"+num);
						ps = conn.prepareStatement("delete from group_group where groupurn = ? and groupdn = ?");
						ps.setString(1, uname);
						ps.setString(2,hname);
						ps.executeUpdate();
					} else if (t.matches("^delegrp[0-9]*") && request.getParameter(t).equals("1")) {
						// delete entitlement mapping
						String num = t.replace("delegrp", "");
						String hname = request.getParameter("egrp"+num);
						String uname = request.getParameter("ename"+num);
						ps = conn.prepareStatement("delete from entitlement_groups where entitlement = ? and groupdn = ?");
						ps.setString(1,uname);
						ps.setString(2,hname);
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("addformsubmitted") != null && request.getParameter("addformsubmitted").equals("1")) {
				// process an add
				if (request.getParameter("lhsselector").equals("eppn")) {
					// user mapping
					ps = conn.prepareStatement("insert into explicit_groups values(?,?)");
					ps.setString(1, request.getParameter("lhs"));
					ps.setString(2, request.getParameter("rhs"));
					ps.executeUpdate();
				} else if (request.getParameter("lhsselector").equals("group")) {
					// group mapping
					ps = conn.prepareStatement("insert into group_group values(?,?)");
					ps.setString(1,request.getParameter("lhs"));
					ps.setString(2, request.getParameter("rhs"));
					ps.executeUpdate();
				} else if (request.getParameter("lhsselector").equals("entitlement")) {
					// entitlement mapping
					ps = conn.prepareStatement("insert into entitlement_groups values(?,?)");
					ps.setString(1, request.getParameter("lhs"));
					ps.setString(2, request.getParameter("rhs"));
					ps.executeUpdate();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed updating database: " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		return retval;
	}
	
	@RequestMapping(value="/host_mgt", method=RequestMethod.POST)
	public ModelAndView handlePostOfHostMgt(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("redirect:https://"+request.getLocalName()+"/admin/host_mgt");
		
		PCAdminConfig config = PCAdminConfig.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed connecting to database: " + e.getMessage());
			}
			if (conn == null) {
				throw new RuntimeException("Failed getting database connection");
			}
			
			if (request.getParameter("accessformsubmitted") != null && request.getParameter("accessformsubmitted").equals("1")) {
				// This is a delete -- remove the relevant value(s)
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^deluhost[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete of a host entry
						String num = t.replace("deluhost", "");
						String hname = request.getParameter("uhost"+num);
						String uname = request.getParameter("uname"+num);
						// delete the entry from the database
						ps = conn.prepareStatement("delete from explicit_hosts where eppn = ? and fqdn = ?");
						ps.setString(1, uname);
						ps.setString(2, hname);
						ps.executeUpdate();
					} else if (t.matches("^deluou[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete ou entry
						String num = t.replace("deluou", "");
						String uname = request.getParameter("uname"+num);
						String ouname = request.getParameter("uou"+num);
						ps = conn.prepareStatement("delete from explicit_hosts where eppn = ? and ou = ?");
						ps.setString(1,uname);
						ps.setString(2,ouname);
						ps.executeUpdate();
					} else if (t.matches("^delghost[0-9]*") && request.getParameter(t).equals("1")) {
						// delete group host
						String num = t.replace("delghost", "");
						String hname = request.getParameter("ghost"+num);
						String gname = request.getParameter("hgrp"+num);
						ps = conn.prepareStatement("delete from group_host where groupurn = ? and fqdn = ?");
						ps.setString(1, gname);
						ps.setString(2,hname);
						ps.executeUpdate();
					} else if (t.matches("^delgou[0-9]*") && request.getParameter(t).equals("1")) {
						// delete group ou
						String num = t.replace("delgou", "");
						String hname = request.getParameter("gou"+num);
						String gname = request.getParameter("hgrp"+num);
						ps = conn.prepareStatement("delete from group_host where groupurn = ? and ou = ?");
						ps.setString(1, gname);
						ps.setString(2, hname);
						ps.executeUpdate();
					} else if (t.matches("^delehost[0-9]*") && request.getParameter(t).equals("1")) {
						// delete entitlement host
						String num = t.replace("delehost", "");
						String hname = request.getParameter("ehost"+num);
						String ename = request.getParameter("hent"+num);
						ps = conn.prepareStatement("delete from entitlement_host where entitlement = ? and fqdn = ?");
						ps.setString(1,ename);
						ps.setString(2,hname);
						ps.executeUpdate();
					} else if (t.matches("^deleou[0-9]*") && request.getParameter(t).equals("1")) {
						// delete entitlement ou
						String num = t.replace("deleou", "");
						String hname = request.getParameter("eou"+num);
						String ename = request.getParameter("hent"+num);
						ps = conn.prepareStatement("delete from entitlement_host where entitlement = ? and ou = ?");
						ps.setString(1, ename);
						ps.setString(2, hname);
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("aformsubmitted") != null && request.getParameter("aformsubmitted").equals("1")) {
				// This is an add -- add the entry
				if (request.getParameter("lhsselector").equals("eppn")) {
					// user entry
					if (request.getParameter("rhsselector").equals("fqdn")) {
						// user -> host entry
						ps = conn.prepareStatement("insert into explicit_hosts values(?,?,?)");
						ps.setString(1, request.getParameter("lhs"));
						ps.setString(2,request.getParameter("rhs"));
						ps.setString(3,null);
						ps.executeUpdate();
					} else if (request.getParameter("rhsselector").equals("oudn")) {
						// user -> ou entry
						ps = conn.prepareStatement("insert into explicit_hosts value(?,?,?)");
						ps.setString(1,request.getParameter("lhs"));
						ps.setString(2, null);
						ps.setString(3,request.getParameter("rhs"));
						ps.executeUpdate();
					}
				} else if (request.getParameter("lhsselector").equals("group")) {
					// group entry
					if (request.getParameter("rhsselector").equals("fqdn")) {
						// group -> host entry
						ps = conn.prepareStatement("insert into group_host values(?,?,?)");
						ps.setString(1, request.getParameter("lhs"));
						ps.setString(2,request.getParameter("rhs"));
						ps.setString(3,null);
						ps.executeUpdate();
					} else if (request.getParameter("rhsselector").equals("oudn")) {
						// group->ou entry
						ps = conn.prepareStatement("insert into group_host values(?,?,?)");
						ps.setString(1,request.getParameter("lhs"));
						ps.setString(2, null);
						ps.setString(3,request.getParameter("rhs"));
						ps.executeUpdate();
					}
				} else if (request.getParameter("lhsselector").equals("entitlement")) {
					// entitlement entry
					if (request.getParameter("rhsselector").equals("fqdn")) {
						// entitlement -> host entry
						ps = conn.prepareStatement("insert into entitlement_host values(?,?,?)");
						ps.setString(1,request.getParameter("lhs"));
						ps.setString(2, request.getParameter("rhs"));
						ps.setString(3,null);
						ps.executeUpdate();
					} else if (request.getParameter("rhsselector").equals("oudn")) {
						// entitlement -> ou entry
						ps = conn.prepareStatement("insert into entitlement_host values(?,?,?)");
						ps.setString(1,request.getParameter("lhs"));
						ps.setString(2, null);
						ps.setString(3, request.getParameter("rhs"));
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("daformsubmitted") != null && request.getParameter("daformsubmitted").equals("1")) {
				// Delete for a DA host
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^deldah[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete of a host entry
						String num = t.replace("deldah", "");
						String hname = request.getParameter("dah"+num);
						// delete the entry from the database
						ps = conn.prepareStatement("delete from domain_admin_hosts where fqdn = ?");
						ps.setString(1, hname);
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("daaformsubmitted") != null && request.getParameter("daaformsubmitted").equals("1")) {
				// Add for a DA host
				ps = conn.prepareStatement("insert into domain_admin_hosts values(?)");
				ps.setString(1, request.getParameter("darhs"));
				ps.executeUpdate();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed updating database: " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		
		return(retval);
	}
	
	@RequestMapping(value="/access_control", method=RequestMethod.POST)
	public ModelAndView handlePostOfAddAC(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("redirect:https://"+request.getLocalName()+"/admin/access_control");
		PCAdminConfig config = PCAdminConfig.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}

		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed connecting to database: " + e.getMessage());
			}
			
			if (conn == null) {
				throw new RuntimeException("Failed getting database connection");
			}
			
			if (request.getParameter("addformsubmitted") != null && request.getParameter("addformsubmitted").equals("1")) {
				// 	add the data
				String selectedType = request.getParameter("typeselector");
				if (selectedType.equals("eppn")) {
					// user entry
					ps = conn.prepareStatement("insert into access_user values(?,?)");
					ps.setString(1, request.getParameter("userproperty"));
					ps.setString(2, "proconsul");
					ps.executeUpdate();
				} else if (selectedType.equals("group")) {
					// group entry
					ps = conn.prepareStatement("insert into access_groups values(?,?,?)");
					ps.setString(1, request.getParameter("userproperty"));
					ps.setString(2,null);
					ps.setString(3, "proconsul");
					ps.executeUpdate();
				} else if (selectedType.equals("entitlement")) {
					// entitlement entry
					ps = conn.prepareStatement("insert into entitlements values(?,?)");
					ps.setString(1,request.getParameter("userproperty"));
					ps.setString(2,"proconsul");
					ps.executeUpdate();
				}
			} else if (request.getParameter("adddaformsubmitted") != null && request.getParameter("adddaformsubmitted").equals("1")) {
				String selectedType = request.getParameter("daselector");
				if (selectedType.equals("eppn")) {
					// user entry
					ps = conn.prepareStatement("insert into access_user values(?,?)");
					ps.setString(1, request.getParameter("daproperty"));
					ps.setString(2, "da");
					ps.executeUpdate();
				} else if (selectedType.equals("group")) {
					// group entry
					ps = conn.prepareStatement("insert into access_groups values(?,?,?)");
					ps.setString(1,request.getParameter("daproperty"));
					ps.setString(2, null);
					ps.setString(3,"da");
					ps.executeUpdate();
				} else if (selectedType.equals("entitlement")) {
					// entitlement entry
					ps = conn.prepareStatement("insert into entitlements values(?,?)");
					ps .setString(1,request.getParameter("daproperty"));
					ps.setString(2, "da");
					ps.executeUpdate();
				}
			} else if (request.getParameter("accessformsubmitted") != null && request.getParameter("accessformsubmitted").equals("1")) {
				//This was a delete.  Look for deletes and process accordingly
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^delpuser[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete of a user entry
						String num = t.replace("delpuser", "");
						String uname = request.getParameter("puser"+num);
						// delete the entry from the database
						ps = conn.prepareStatement("delete from access_user where eppn = ? and type = 'proconsul'");
						ps.setString(1, uname);
						ps.executeUpdate();
					} else if (t.matches("^delpgrp[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete group entry
						String num = t.replace("delpgrp", "");
						String uname = request.getParameter("pgrp"+num);
						ps = conn.prepareStatement("delete from access_groups where groupurn = ? and type = 'proconsul'");
						ps.setString(1,uname);
						ps.executeUpdate();
					} else if (t.matches("^delpent[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete entitlement entry
						String num = t.replace("delpent", "");
						String uname = request.getParameter("pent"+num);
						ps = conn.prepareStatement("delete from entitlements where entitlement = ? and target = 'proconsul'");
						ps.setString(1, uname);
						ps.executeUpdate();
					}
				}
			} else if (request.getParameter("daaformsubmitted") != null && request.getParameter("daaformsubmitted").equals("1")) {
				//This was a dadelete.  Look for dadeletes and process accordingly
				Enumeration<String> pnames = request.getParameterNames();
				while (pnames != null && pnames.hasMoreElements()) {
					String t = pnames.nextElement();
					if (t.matches("^deldauser[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete of a user entry
						String num = t.replace("deldauser", "");
						String uname = request.getParameter("dauser"+num);
						// delete the entry from the database
						ps = conn.prepareStatement("delete from access_user where eppn = ? and type = 'da'");
						ps.setString(1, uname);
						ps.executeUpdate();
					} else if (t.matches("^deldagrp[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete group entry
						String num = t.replace("deldagrp", "");
						String uname = request.getParameter("dagrp"+num);
						ps = conn.prepareStatement("delete from access_groups where groupurn = ? and type = 'da'");
						ps.setString(1,uname);
						ps.executeUpdate();
					} else if (t.matches("^deldaent[0-9]*") && request.getParameter(t).equals("1")) {
						// This is a delete entitlement entry
						String num = t.replace("deldaent", "");
						String uname = request.getParameter("daent"+num);
						ps = conn.prepareStatement("delete from entitlements where entitlement = ? and target = 'da'");
						ps.setString(1, uname);
						ps.executeUpdate();
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed updating database: " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		return retval;
	}
	
	@RequestMapping(value="/host_and_gateway_details", method=RequestMethod.GET)
	public ModelAndView generateHostGatewayDeatils(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("gateway");
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		PCAdminConfig config = PCAdminConfig.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		ArrayList<HostGateway> hga = new ArrayList<HostGateway>();
		ArrayList<HostAccessGroup> haga = new ArrayList<HostAccessGroup>();
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed connecting to database: " + e.getMessage());
			}
			
			if (conn == null) {
				throw new RuntimeException("Failed retriving connection to database");
			}
			
			ps = conn.prepareStatement("select fqdn,ou,groupdn from host_access_group");
			if (ps == null) {
				throw new RuntimeException("Select failed for host access groups");
			}
			rs = ps.executeQuery();
			while (rs != null && rs.next()) {
				HostAccessGroup hag = new HostAccessGroup();
				hag.setFqdn(rs.getString("fqdn"));
				hag.setOu(rs.getString("ou"));
				hag.setGroupdn(rs.getString("groupdn"));
				haga.add(hag);
			}
			ps.close();
			if (rs != null)
				rs.close();
			
			ps = conn.prepareStatement("select fqdn,gateway,groupdn from host_gateway");
			if (ps == null) {
				throw new RuntimeException("Select failed for gateway information");
			}
			rs = ps.executeQuery();
			while (rs != null && rs.next()) {
				HostGateway hg = new HostGateway();
				hg.setFqdn(rs.getString("fqdn"));
				hg.setGateway(rs.getString("gateway"));
				hg.setGroupdn(rs.getString("groupdn"));
				hga.add(hg);
			}
			ps.close();
			if (rs!=null)
				rs.close();
			
		} catch (Exception e) {
			ModelAndView erret = new ModelAndView("test");
			erret.addObject("message","Failed retrieving host gateway and group data: " + e.getMessage());
			return erret;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		
		retval.addObject("hostgateway",hga);
		retval.addObject("hostaccessgroups",haga);
	
		return retval;
	}
	
	@RequestMapping(value="/windows_users", method=RequestMethod.GET)
	public ModelAndView generateWindowsUsers(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("windows_users");
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		
		PCAdminConfig config = PCAdminConfig.getInstance();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		ArrayList<UserGroupMapping> ugm = new ArrayList<UserGroupMapping>();
		ArrayList<GroupGroupMapping> ggm = new ArrayList<GroupGroupMapping>();
		ArrayList<EntitlementGroupMapping> egm = new ArrayList<EntitlementGroupMapping>();
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed getting PC AdminDB connection: " + e.getMessage());
			}
			if (conn != null) {
				ps = conn.prepareStatement("select eppn,groupdn from explicit_groups");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for explicit groups");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					UserGroupMapping ug = new UserGroupMapping();
					ug.setEppn(rs.getString("eppn"));
					ug.setGroupdn(rs.getString("groupdn"));
					ugm.add(ug);
				}
				ps.close();
				if (rs != null)
					rs.close();
				
				ps = conn.prepareStatement("select groupurn,groupdn from group_group");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for group groups");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					GroupGroupMapping gg = new GroupGroupMapping();
					gg.setGroupurn(rs.getString("groupurn"));
					gg.setGroupdn(rs.getString("groupdn"));
					ggm.add(gg);
				}
				ps.close();
				if (rs != null)
					rs.close();
				
				ps = conn.prepareStatement("select entitlement,groupdn from entitlement_groups");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for entitlement groups");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					EntitlementGroupMapping eg = new EntitlementGroupMapping();
					eg.setEntitlement(rs.getString("entitlement"));
					eg.setGroupdn(rs.getString("groupdn"));
					egm.add(eg);
				}
				ps.close();
				if (rs != null) 
					rs.close();
			}
		} catch (Exception e) {
			ModelAndView erret = new ModelAndView();
			erret.addObject("message","Failed retrieving group mapping information: " + e.getMessage());
			return erret;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		retval.addObject("usergroups",ugm);
		retval.addObject("groupgroups",ggm);
		retval.addObject("entitlementgroups",egm);
		
		return retval;
	}
	
	@RequestMapping(value="/host_mgt", method=RequestMethod.GET)
	public ModelAndView generateHostManagement(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("host_mgt");
		
		// Authorize
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		
		// Marshalling
		PCAdminConfig config = PCAdminConfig.getInstance();
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		ArrayList<UserHostMapping> uhm = new ArrayList<UserHostMapping>();
		ArrayList<GroupHostMapping> ghm = new ArrayList<GroupHostMapping>();
		ArrayList<EntitlementHostMapping> ehm = new ArrayList<EntitlementHostMapping>();
		ArrayList<String> dahosts = new ArrayList<String>();
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed in getPCAdminDBConnection()" + e.getMessage());
			}
			if (conn != null) {
				// marshall the user host mappings to fqdns
				ps = conn.prepareStatement("select eppn,fqdn from explicit_hosts where fqdn is not null");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for explicit user fqdns");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					UserHostMapping um = new UserHostMapping();
					um.setEppn(rs.getString("eppn"));
					um.setFqdn(rs.getString("fqdn"));
					uhm.add(um);
				}
				ps.close(); // never null
				if (rs != null) 
					rs.close();  // may be null
				
				// marshall user host mappings to OUs
				ps = conn.prepareStatement("select eppn,ou from explicit_hosts where ou is not null");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for explicit user ous");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					UserHostMapping uo = new UserHostMapping();
					uo.setEppn(rs.getString("eppn"));
					uo.setOudn(rs.getString("ou"));
					uhm.add(uo);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				// marshall group host mappings to fqdns
				ps = conn.prepareStatement("select groupurn,fqdn from group_host where fqdn is not null");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for group fqdn map");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					GroupHostMapping gm = new GroupHostMapping();
					gm.setGroupurn(rs.getString("groupurn"));
					gm.setFqdn(rs.getString("fqdn"));
					ghm.add(gm);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				// marshall group host mappings to ous
				ps = conn.prepareStatement("select groupurn,ou from group_host where ou is not null");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for group ou map");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					GroupHostMapping go = new GroupHostMapping();
					go.setGroupurn(rs.getString("groupurn"));
					go.setOudn(rs.getString("ou"));
					ghm.add(go);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				// marshall entitlement host mappings to fqdns
				ps = conn.prepareStatement("select entitlement,fqdn from entitlement_host where fqdn is not null");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for entitlement fqdn map");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					EntitlementHostMapping em = new EntitlementHostMapping();
					em.setEntitlement(rs.getString("entitlement"));
					em.setFqdn(rs.getString("fqdn"));
					ehm.add(em);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				// marshall entitlement host mappings to ous
				ps = conn.prepareStatement("select entitlement,ou from entitlement_host where ou is not null");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for entitlement ou map");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					EntitlementHostMapping eo = new EntitlementHostMapping();
					eo.setEntitlement(rs.getString("entitlement"));
					eo.setOudn(rs.getString("ou"));
					ehm.add(eo);
				}
				ps.close();
				if (rs != null)
					rs.close();
				
				// And finally, da host list
				ps = conn.prepareStatement("select fqdn from domain_admin_hosts");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for domain admin hosts");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					dahosts.add(rs.getString("fqdn"));
				}
				ps.close();
				if (rs != null) 
					rs.close();
			}
		} catch (Exception e) {
			ModelAndView erret = new ModelAndView("test");
			erret.addObject("message","Error reading from DB: "+e.getMessage());
			return erret;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
				
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		retval.addObject("userhosts",uhm);
		retval.addObject("grouphosts",ghm);
		retval.addObject("entitlementhosts",ehm);
		retval.addObject("dahosts",dahosts);
		
		return(retval);
	}

	@RequestMapping(value="/access_control", method=RequestMethod.GET)
	public ModelAndView generateAccessControl(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("access_control");
	
		// Authorize
		if (! isAdmin(request)) {
			ModelAndView eret = new ModelAndView("test");
			eret.addObject("message","You are not authorized to perform that operation.");
			return eret;
		}
		// Marshall the contents of three tables into six components
		PCAdminConfig config = PCAdminConfig.getInstance();
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		
		ArrayList<AccessGroupEntry> ags = new ArrayList<AccessGroupEntry>();
		ArrayList<AccessGroupEntry> agdas = new ArrayList<AccessGroupEntry>();
		ArrayList<AccessEntitlementEntry> aes = new ArrayList<AccessEntitlementEntry>();
		ArrayList<AccessEntitlementEntry> aedas = new ArrayList<AccessEntitlementEntry>();
		ArrayList<AccessUserEntry> aus = new ArrayList<AccessUserEntry>();
		ArrayList<AccessUserEntry> audas = new ArrayList<AccessUserEntry>();
		
		try {
			try {
				conn = DatabaseConnectionFactory.getPCAdminDBConnection();
			} catch (Exception e) {
				throw new RuntimeException("Failed in getPCAdminDBConnection()" + e.getMessage());
			}
			if (conn != null) {
				// marshall the base groups
				ps = conn.prepareStatement("select groupurn,ou,type from access_groups where type = 'proconsul'");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for group access list");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					AccessGroupEntry age = new AccessGroupEntry();
					age.setGroupurn(rs.getString("groupurn"));
					age.setOu(rs.getString("ou"));
					age.setType("proconsul");
					ags.add(age);
				}
				ps.close(); // never null
				if (rs != null) 
					rs.close();  // may be null 
				
				// and the DA groups
				ps = conn.prepareStatement("select groupurn,ou,type from access_groups where type = 'da'");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for group DA list");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					AccessGroupEntry da = new AccessGroupEntry();
					da.setGroupurn(rs.getString("groupurn"));
					da.setOu(rs.getString("ou"));
					da.setType("da");
					agdas.add(da);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				// And the same for entitlements
				ps = conn.prepareStatement("select entitlement,target from entitlements where target = 'proconsul'");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for general entitlements");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					AccessEntitlementEntry ae = new AccessEntitlementEntry();
					ae.setEntitlement(rs.getString("entitlement"));
					ae.setTarget("proconsul");
					aes.add(ae);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				ps = conn.prepareStatement("select entitlement,target from entitlements where target = 'da'");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for da entitlements");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					AccessEntitlementEntry ae = new AccessEntitlementEntry();
					ae.setEntitlement(rs.getString("entitlement"));
					ae.setTarget("da");
					aedas.add(ae);
				}
				ps.close();
				if (rs != null)
					rs.close();
				
				// and the same for individual users
				ps = conn.prepareStatement("select eppn,type from access_user where type = 'proconsul'");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for proconsul user list");
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					AccessUserEntry au = new AccessUserEntry();
					au.setEppn(rs.getString("eppn"));
					au.setType(rs.getString("type"));
					aus.add(au);
				}
				ps.close();
				if (rs != null) 
					rs.close();
				
				ps = conn.prepareStatement("select eppn,type from access_user where type = 'da'");
				if (ps == null) {
					throw new RuntimeException("Failed to perform select for da user list"); 
				}
				rs = ps.executeQuery();
				while (rs != null && rs.next()) {
					AccessUserEntry au = new AccessUserEntry();
					au.setEppn(rs.getString("eppn"));
					au.setType(rs.getString("type"));
					audas.add(au);
				}
				ps.close();
				if (rs != null)
					rs.close();
			}
		} catch (Exception e) {
			ModelAndView erret = new ModelAndView("test");
			erret.addObject("message","Error reading from DB: "+e.getMessage());
			return erret;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		retval.addObject("proconsul_groups",ags);
		retval.addObject("da_groups",agdas);
		retval.addObject("proconsul_entitlements",aes);
		retval.addObject("da_entitlements",aedas);
		retval.addObject("proconsul_users",aus);
		retval.addObject("da_users",audas);
		
		return(retval);
	}
	
	@RequestMapping(value="/",method=RequestMethod.GET)
	public ModelAndView dohome(HttpServletRequest request) {
		ModelAndView retval = new ModelAndView("main");
		
		// No data to embed -- just return the main vm as is
		return retval;
	}
	
	
}
