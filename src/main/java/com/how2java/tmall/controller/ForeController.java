package com.how2java.tmall.controller;

import com.how2java.tmall.pojo.*;
import com.how2java.tmall.service.*;
import com.how2java.tmall.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpSession;
import java.io.Console;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("")
public class ForeController {
    @Autowired
    CategoryService categoryService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    OrderService orderService;
    @Autowired
    ProductService productService;
    @Autowired
    ProductImageService productImageService;
    @Autowired
    PropertyService propertyService;
    @Autowired
    PropertyValueService propertyValueService;
    @Autowired
    UserService userService;
    @Autowired
    ReviewService reviewService;

    //首页
    @RequestMapping("forehome")
    public ModelAndView home(){
        ModelAndView mav=new ModelAndView();
//		所需要的数据：所有分类列表cs，并带有产品信息
//      所显示的视图：home.jsp
        List<Category> cs=categoryService.list();
        productService.fill(cs);//配置p
        productService.fillByRow(cs);//配置pByRow

        mav.addObject("cs",cs);
        mav.setViewName("fore/home");
        return  mav;
    }
    //注册
    @RequestMapping("foreregister")
    public ModelAndView register(User user){
        ModelAndView mav=new ModelAndView();
        //判断用户已经存在，返回msg "用户名已经被使用,不能使用"

        //为了页面显示用户名时防止恶意符号，要进行对名字的转义
        user.setName(HtmlUtils.htmlEscape(user.getName()));

        if(!userService.hasUser(user)){//可以注册，注册成功跳转到注册成功页面
            userService.add(user);
            mav.setViewName("redirect:/registerSuccessPage");//forePageNontroller响应这个路径
        }
        else{//不可注册，跳转原来页面
            String msg="用户名已经被使用,不能使用";
            mav.addObject("msg",msg);
            mav.setViewName("fore/register");//服务器跳转register.jsp
        }
        //由于user作为参数，会被隐性地加入到mav里,导致top.jsp里取得到user对象。所以要置空
        mav.addObject("user",null);
        return mav;
    }
    //登录
    @RequestMapping("forelogin")
    public ModelAndView login(User user, HttpSession session){
        ModelAndView mav=new ModelAndView();

        //为了页面显示用户名时防止恶意符号，要进行对名字的转义
        user.setName(HtmlUtils.htmlEscape(user.getName()));
        User u=userService.getUser(user.getName(),user.getPassword());//数据库找到的user

        session.setAttribute("user",u);//设置session的用户

        if(u!=null){//账户密码正确
            //设置user、cartTotalItemNumber
            mav.setViewName("redirect:/forehome");
        }
        else{//不成功
            String msg=null;
            if(!userService.hasUser(user)){//不存在该用户名
                msg="该用户不存在";//服务器跳转，msg:该用户不存在
            }
            else{
                msg="密码错误";// 服务器跳转，msg:密码不正确
            }
            mav.setViewName("fore/login");
            mav.addObject("msg",msg);
            //由于user作为参数，会被隐性地加入到mav里,导致top.jsp里取得到user对象。所以要置空
            mav.addObject("user",null);
        }
        return  mav;
    }
    //退出
    @RequestMapping("forelogout")
    public ModelAndView logout(HttpSession session){
        //session去掉user，跳转到首页
        ModelAndView mav=new ModelAndView("redirect:/forehome");
        session.removeAttribute("user");
        return mav;
    }
    //产品页
    @RequestMapping("foreproduct")
    public ModelAndView product(int pid){
        ModelAndView mav =new ModelAndView();
        //需要传pvs、p、reviews
        Product product=productService.get(pid);//设置了非数据库字段
        productService.setProductDetailImages(product);
        productService.setProductSingleImages(product);

        List<PropertyValue> pvs=propertyValueService.list(pid);//设置了pt
        List<Review> reviews=reviewService.list(pid);//设置了user
        productService.setSaleAndReviewNumber(product);//设置了销量评论数
        mav.addObject("pvs",pvs);
        mav.addObject("p",product);
        mav.addObject("reviews",reviews);

        mav.setViewName("fore/product");
        return mav;
    }
    /*productImgAndInfo.jsp传来*/
    @RequestMapping("forecheckLogin")
    @ResponseBody//响应体中的数据
    public String checkLogin(HttpSession session){//判断此时是否在登录-模态登录
        User user =(User)session.getAttribute("user");
        if(null==user){//没有在登录
            return "failed";
        }
        else
            return "success";
    }

    @RequestMapping("foreloginAjax")
    @ResponseBody
    public String loginAjax(String name,String password,HttpSession session){
        name=HtmlUtils.htmlEscape(name);
        User user=userService.getUser(name,password);

        if(null!=user){//登录成功
            session.setAttribute("user",user);
            return "success";
        }
        else{
            return "failed";
        }
    }

    //分类页
    @RequestMapping("forecategory")
    public ModelAndView category(int cid,String sort){
        ModelAndView mav=new ModelAndView("fore/category");
        //需要填充好ps的c ps也要填充
        Category c=categoryService.get(cid);
        productService.fill(c);
        productService.setSaleAndReviewNumber(c.getProducts());
        //排序
        if(null!=sort){
            switch (sort){
                case "all":
                    Collections.sort(c.getProducts(), new ComparatorAll());
                    break;
                case "review":
                    Collections.sort(c.getProducts(), new ComparatorReview());
                    break;
                case "date":
                    Collections.sort(c.getProducts(), new ComparatorDate());
                    break;
                case "saleCount":
                    Collections.sort(c.getProducts(), new ComparatorSaleCount());
                    break;
                case "price":
                    Collections.sort(c.getProducts(), new ComparatorPrice());
                    break;
                default:break;
            }
        }
        mav.addObject("c",c);
        return mav;
    }
    //搜索
    @RequestMapping("foresearch")
    public ModelAndView search(String keyword,String sort){//搜索产品标题
        ModelAndView mav=new ModelAndView("fore/searchResult");
        //传递ps
        List<Product> ps=productService.searchProuducts(keyword);
        productService.setSaleAndReviewNumber(ps);

        //排序
        if(null!=sort){
            switch (sort){
                case "all":
                    Collections.sort(ps, new ComparatorAll());
                    break;
                case "review":
                    Collections.sort(ps, new ComparatorReview());
                    break;
                case "date":
                    Collections.sort(ps, new ComparatorDate());
                    break;
                case "saleCount":
                    Collections.sort(ps, new ComparatorSaleCount());
                    break;
                case "price":
                    Collections.sort(ps, new ComparatorPrice());
                    break;
                default:break;
            }
        }
        mav.addObject("keyword",keyword);
        mav.addObject("ps",ps);
        return mav;
    }
    //用户登录才能访问
    //加入购物车 新增 OrderItem
    @RequestMapping("foreaddCart")
    @ResponseBody
    public String addCart(int pid,int num){//num:加入购物车的数量
        return "";
    }
    //立即购买 新增 OrderItem
    @RequestMapping("forebuyone")
    public ModelAndView buyone(int pid,int num,HttpSession session){
        ModelAndView mav=new ModelAndView();
        //需要填充了p的ois、付款总金额sum
        //a. 如果已经存在这个产品对应的OrderItem，并且还没有生成订单，即还在购物车中。 那么就应该在对应的OrderItem基础上，调整数量
        //a.1 基于用户对象user，查询没有生成订单的某产品的订单项
        //a.2 如果存在该订单项的话，就进行数量追加
        //b. 如果不存在对应的OrderItem,那么就新增一个订单项OrderItem
        //b.1 生成新的订单项
        //b.2 设置数量，用户和产品
        //b.3 插入到数据库
        //bug:结算时会把购物车里同一产品的订单项也算进去
        // 要解决要给oi添加type字段。区分订单项是来源于立即购买还是购物车----逆向工程的问题
        Product p=productService.get(pid);
        User user=(User)session.getAttribute("user");
        OrderItem oi=orderItemService.getUser_Product_notOrder(user.getId(),pid);//没有设置p
        if(oi==null){
            oi=new OrderItem();
            oi.setNumber(num);
            oi.setOid(-1);//未形成订单
            oi.setPid(p.getId());
            oi.setUid(user.getId());
            orderItemService.add(oi);//加到数据库//加到了购物车里
        }else{
            oi.setNumber(oi.getNumber()+num);
            orderItemService.update(oi);//更新数据库
        }
        int oiid=oi.getId();//加到数据库后，自动将原来对象的id填充
        mav.setViewName("redirect:forebuy?oiid="+oiid);
        return mav;
    }
    //结算页面
    @RequestMapping("forebuy")
    public ModelAndView buy(int[] oiid,HttpSession session){
        ModelAndView mav=new ModelAndView("fore/buy");
        List<OrderItem> ois = new ArrayList<>();
        float total = 0;
        for (int i=0;i<oiid.length;i++) {
            int id = oiid[i];
            OrderItem oi= orderItemService.get(id);
            total +=oi.getProduct().getPromotePrice()*oi.getNumber();
            ois.add(oi);
        }
        session.setAttribute("ois", ois);
        mav.addObject("sum", total);
        return mav;
    }

}

