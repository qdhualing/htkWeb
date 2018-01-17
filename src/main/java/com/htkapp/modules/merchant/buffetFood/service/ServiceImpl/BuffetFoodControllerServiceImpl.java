package com.htkapp.modules.merchant.buffetFood.service.ServiceImpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.htkapp.core.LogUtil;
import com.htkapp.core.OtherUtils;
import com.htkapp.core.jsAjax.AjaxResponseModel;
import com.htkapp.core.params.AjaxRequestParams;
import com.htkapp.core.utils.FileUploadUtils;
import com.htkapp.core.utils.Globals;
import com.htkapp.modules.common.entity.LoginUser;
import com.htkapp.modules.merchant.buffetFood.dao.BuffetFoodOrderMapper;
import com.htkapp.modules.merchant.buffetFood.dto.ReturnCategoryAndProductList;
import com.htkapp.modules.merchant.buffetFood.entity.BuffetFoodCategory;
import com.htkapp.modules.merchant.buffetFood.entity.BuffetFoodOrder;
import com.htkapp.modules.merchant.buffetFood.entity.BuffetFoodOrderProduct;
import com.htkapp.modules.merchant.buffetFood.entity.BuffetFoodProduct;
import com.htkapp.modules.merchant.buffetFood.service.*;
import com.htkapp.modules.merchant.printThis.PrintTest;
import com.htkapp.modules.merchant.shop.entity.Shop;
import com.htkapp.modules.merchant.shop.service.ShopServiceI;

import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;


@Service
public class BuffetFoodControllerServiceImpl implements BuffetFoodControllerService {

	@Resource
	private BuffetFoodCategoryService buffetFoodCategoryService;
	@Resource
	private BuffetFoodProductService buffetFoodProductService;
	@Resource
	private ShopServiceI shopService;
	@Resource
	private BuffetFoodOrderService buffetFoodOrderService;
	@Resource
	private BuffetFoodOrderProductService buffetFoodOrderProductService;
	@Resource
	private BuffetFoodOrderMapper buffetFoodOrderDao;

	private Class<? extends Object> cls = BuffetFoodControllerServiceImpl.class;


	/* =========================接口开始============================ */
	//添加产品页面
	@Override
	public void addProduct(BuffetFoodProduct product, MultipartFile imgFile) {
		try {
			//处理产品图片
			if (imgFile != null) {
				String uploadUrl = FileUploadUtils.appUploadAvatarImg(imgFile, "shop/buffetFood/");
				product.setImgUrl(uploadUrl);
			}
			LoginUser user = OtherUtils.getLoginUserByRequest();
			product.getMark();
			Shop shop = shopService.getShopIdByAccountShopId(user.getUserId(),product.getMark());
			product.setShopId(shop.getShopId());
			buffetFoodProductService.addBuffetFoodProduct(product);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.error(cls, e.getMessage(), e);
		}
	}

	//通过商户ID获取分类和商品接口
	@Override
	public AjaxResponseModel getCategoryAndProductByAccountShopId(int accountShopId) {
		//通过商户ID获取分类和商品;
		try {
			//获取分类
			List<BuffetFoodCategory> categoryList = buffetFoodCategoryService.getBuffetFoodCategoryListByAccountShopId(accountShopId);
			if (categoryList == null) {
				//分类为空，则返回
				return new AjaxResponseModel<>(Globals.API_SUCCESS, "分类列表为空", null);
			}
			List<ReturnCategoryAndProductList> data = new ArrayList<>();
			for (BuffetFoodCategory each : categoryList) {
				//循环分类集合，通过分类ID获取商品
				List<BuffetFoodProduct> productList = buffetFoodProductService.getBuffetFoodProductById(each.getId());
				if (productList != null) {
					for (BuffetFoodProduct every : productList) {
						every.setImgUrl(OtherUtils.getRootDirectory() + every.getImgUrl());
					}
				}
				//添加分类和商品集合
				data.add(new ReturnCategoryAndProductList(each.getCategoryName(), each.getId(), productList));
			}
			return new AjaxResponseModel<>(Globals.COMMON_SUCCESSFUL_OPERATION, "成功", data);
		} catch (Exception e) {
			return new AjaxResponseModel<>(Globals.COMMON_OPERATION_FAILED, "捕获到异常", null);
		}
	}

	//删除分类接口
	@Override
	public AjaxResponseModel delCategoryById(int categoryId) {
		//先删除分类下的商品，再删除分类
		try {
			buffetFoodProductService.delProductByCId(categoryId);
			System.out.println("删除分类和分类下的所有商品");
			buffetFoodCategoryService.delCategoryById(categoryId);
			return new AjaxResponseModel<>(Globals.COMMON_SUCCESSFUL_OPERATION, "删除成功");
		} catch (Exception e) {
			//手动回滚事务
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return new AjaxResponseModel<>(Globals.COMMON_OPERATION_FAILED, "删除失败");
		}
	}

	//通过商户ID获取分类列表
	@Override
	public void getCategoryListById(Model model) {
		try {
			LoginUser user = OtherUtils.getLoginUserByRequest();
			List<BuffetFoodCategory> resultList = buffetFoodCategoryService.getBuffetFoodCategoryListByAccountShopId(user.getUserId());
			if (resultList == null) {
				return;
			}
			model.addAttribute("data", resultList);
		} catch (Exception e) {
			model.addAttribute("data", null);
		}
	}

	//通过产品id查找出产品信息
	@Override
	public void getProductDetailByPID(Model model, int productId) {
		//查找商品信息 model返回给前台
		try {
			LoginUser user = OtherUtils.getLoginUserByRequest();
			List<BuffetFoodCategory> resultList = buffetFoodCategoryService.getBuffetFoodCategoryListByAccountShopId(user.getUserId());
			if (resultList == null) {
				return;
			}
			model.addAttribute("data", resultList);
			BuffetFoodProduct product = buffetFoodProductService.getBuffetFoodProductByPId(user.getUserId(), productId);
			if (product != null) {
				product.setImgUrl(OtherUtils.getRootDirectory() + product.getImgUrl());
			}
			model.addAttribute("dataPro", product);
		} catch (Exception e) {
			model.addAttribute("dataPro", null);
		}
	}

	//保存商品修改
	@Override
	public void saveProductEdit(BuffetFoodProduct product) throws Exception {
		try {
			buffetFoodProductService.editProductById(product);
		} catch (Exception e) {
			throw new Exception("保存修改失败");
		}
	}

	//新订单下单接口
	@Override
	public AjaxResponseModel dealWithNewOrder(AjaxRequestParams params) {
		if(params != null && params.getOrderNumber() != null){
			try {
				buffetFoodOrderService.dealWithNewOrder(params.getOrderNumber(),1,1);
				return new AjaxResponseModel(Globals.COMMON_SUCCESSFUL_OPERATION);
			}catch (Exception e){
				return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
			}
		}else {
			return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
		}
	}

	//回复催单接口
	@Override
	public AjaxResponseModel replyReminder(AjaxRequestParams params) {
		if(params != null && params.getOrderNumber() != null){
			try {
				buffetFoodOrderService.replyReminder(params.getOrderNumber(),0);
				return new AjaxResponseModel(Globals.COMMON_SUCCESSFUL_OPERATION);
			}catch (Exception e){
				return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
			}
		}else {
			return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
		}
	}

	//确认调单
	@Override
	public AjaxResponseModel enterAdjust(AjaxRequestParams params) {
		if(params != null && params.getOrderNumber() != null){
			try {
				//取出 要调整商品的json字符串
				BuffetFoodOrder order = buffetFoodOrderService.getBuffetFoodOrderByOrderNumber(params.getOrderNumber());
				if(order != null && order.getAdjustOrderProductJson() != null){
					Gson gson = new Gson();
					List<BuffetFoodOrderProduct> productLists = gson.fromJson(order.getAdjustOrderProductJson(), new TypeToken<List<BuffetFoodOrderProduct>>() {
					}.getType());
					if (productLists != null && productLists.size() > 0){
						//删除订单下的原商品信息
						buffetFoodOrderProductService.deleteOrderProductByOrderId(order.getId());
						//插入修改后的订单下商品信息
						double orderAmount = 0.00;
						for (BuffetFoodOrderProduct each : productLists){
							orderAmount += each.getPrice() * each.getQuantity();
							each.setOrderId(order.getId());
							buffetFoodOrderProductService.insertProductDetailsUnderOrder(each);
						}
						buffetFoodOrderService.updateOrderTotalAmount(order.getOrderNumber(), orderAmount);
						int row = buffetFoodOrderDao.replyFalseDAO(params.getOrderNumber(),null);
						if(row <= 0){
							return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
						}else {
							return new AjaxResponseModel(Globals.COMMON_SUCCESSFUL_OPERATION);
						}
					}else {
						return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
					}
				}else {
					return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
				}
			}catch (Exception e){
				System.out.println(e.getMessage());
				return new AjaxResponseModel(Globals.COMMON_OPERATION_FAILED);
			}
		}else {
			return new AjaxResponseModel(Globals.COMMON_PARAMETER_ERROR);
		}
	}

	@Override
	public AjaxResponseModel printOrder(AjaxRequestParams params) {
		//TODO
		if(params != null && params.getOrderNumber() != null){
			List<BuffetFoodOrderProduct> list=new ArrayList<BuffetFoodOrderProduct>();
			try {
				//获取订单编号
				String OderNumeber=params.getOrderNumber();
				//查询订单详情
				BuffetFoodOrder bfo=buffetFoodOrderDao.getBuffetFoodOrderByOrderNumberDAO(OderNumeber);
				String orderDesc = "gmt_create desc";
				int pageNumber = Globals.DEFAULT_PAGE_NO;
				int pageLimit = Globals.DEFAULT_PAGE_LIMIT;
				//通过订单详情的id查询订单内容
				List<BuffetFoodOrder> result = buffetFoodOrderService.getAdjustOrderList(bfo.getShopId(), orderDesc, pageNumber, pageLimit);
				for(BuffetFoodOrder buffetFoodOrder:result) {
					 list=buffetFoodOrderProductService.getOrderProductListById(buffetFoodOrder.getId());
				}
		        //    通俗理解就是书、文档
		        Book book = new Book();
		        //    设置成竖打
		        PageFormat pf = new PageFormat();
		        pf.setOrientation(PageFormat.PORTRAIT);

		        //    通过Paper设置页面的空白边距和可打印区域。必须与实际打印纸张大小相符。
		        Paper p = new Paper();
		        p.setSize(590, 840);//纸张大小
		        p.setImageableArea(10, 10, 590, 840);//A4(595 X 842)设置打印区域，其实0，0应该是72，72，因为A4纸的默认X,Y边距是72
		        pf.setPaper(p);

		        //    把 PageFormat 和 Printable 添加到书中，组成一个页面
		        PrintTest pt=new PrintTest();
		        pt.setList(list);
		        book.append(pt, pf);
		        //获取打印服务对象
		        PrinterJob job = PrinterJob.getPrinterJob();

		        // 设置打印类
		        job.setPageable(book);
		        job.print();

			}catch(Exception e) {
				return new AjaxResponseModel<>(Globals.COMMON_OPERATION_FAILED,"打印失败");
			}
		}
		return new AjaxResponseModel<>(Globals.COMMON_SUCCESSFUL_OPERATION,"打印成功");
	}

	/* =========================接口结束============================ */
}
