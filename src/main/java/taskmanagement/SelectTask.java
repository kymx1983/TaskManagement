package taskmanagement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import util.TaskUtil;

@RestController
public class SelectTask {

  @Autowired
  TaskDataRepository repository;

  @Autowired
  private TaskDataService service;

  @Autowired
  private UserDataService userService;

  @Autowired
  private TaskDataService taskService;

  @Autowired
  private StatusDataService statusService;

  /**
   * 検索処理.
   * @param searchDate 画面入力された検索日
   * @param searchText 画面入力された検索文字列
   * @param statusConditions 画面入力された検索ステータス
   * @param mav 画面入力値
   * @return
   */
  @RequestMapping(value = "/select", method = RequestMethod.POST)
  public ModelAndView select(
      @RequestParam String searchDate,
      @RequestParam String searchText,
      @RequestParam(value = "statusConditions", required = false) String[] statusConditions,
      ModelAndView mav) {

    mav.setViewName("selectTask.html");

    mav.addObject("searchDate", TaskUtil.getToday());

    System.out.println("searchDate" + searchDate);

    StringBuffer status = new StringBuffer();
    if (statusConditions != null) {
      for (String value : statusConditions) {
        if (status.length() > 0) {
          status.append(",");
        }
        status.append(value);
      }
    }

    List<TaskData> list = service.searchTask(searchDate, searchText, status.toString());
    mav.addObject("datalist", list);

    return mav;
  }

  /**
   * タスク編集.
   * @param taskNo タスクNo
   * @param mav ModelAndView
   * @return
   */
  @RequestMapping(value = "/edit", method = RequestMethod.POST)
  public ModelAndView editTask(@RequestParam long taskNo, ModelAndView mav) {

    mav = getInsertTaskView(taskNo);
    mav.addObject("obj", getTaskData(taskNo));

    return mav;

  }

  /**
   * 更新処理.
   * @param taskData タスク情報
   * @param result BindingResult
   * @param mode 更新モード
   * @return
   */
  @RequestMapping(value = "/flush", method = RequestMethod.POST)
  @Transactional(readOnly = false)
  public ModelAndView flush(
      @ModelAttribute("obj") @Validated TaskData taskData,
      BindingResult result,
      @RequestParam String mode) {

    if (result.hasErrors()) {
      ModelAndView mav = getInsertTaskView(taskData.getTaskNo());
      mav.addObject("message", "入力内容に誤りがあります");
      mav.addObject("obj", taskData);
      return mav;

    }

    switch (mode) {
      case "insert":
      case "update":
        repository.saveAndFlush(taskData);
        break;
      case "copy":
        taskData.setTaskNo(0);
        repository.saveAndFlush(taskData);
        break;
      case "delete":
        repository.delete(taskData);
        break;
      default:
        break;
    }

    // ログイン成功
    ModelAndView mav = new ModelAndView();
    mav.setViewName("SelectTask.html");
    mav.addObject("searchDate", TaskUtil.getToday());
    List<TaskData> list = taskService.getAll();
    mav.addObject("datalist", list);

    List<UserData> user = userService.getAll();
    mav.addObject("userList", user);

    return mav;
  }

  /**
   * insertTaskの検索を行う.
   * @param taskNo InsertTaskの対象とするtaskNoを設定する
   * @return insertTaskの表示対象となるModelAndViewインスタンス
   */
  public ModelAndView getInsertTaskView(long taskNo) {

    ModelAndView mav = new ModelAndView();

    mav.setViewName("InsertTask.html");

    // ステータスのラジオボタンに表示する内容を設定する
    mav.addObject("statusItem", getStatusItem());

    boolean modeVisible = false;
    if (taskNo != 0) {
      modeVisible = true;
    }
    mav.addObject("modeVisible", modeVisible);

    return mav;

  }

  private TaskData getTaskData(long taskNo) {
    TaskData data = new TaskData();
    if (taskNo != 0) {
      data = service.findByTaskNo(taskNo);
    } else {
      // 新規作成の場合
      // 日付項目に今日日付を設定する
      String today = TaskUtil.getToday();
      data.setPlanFrom(today);
      data.setPlanTo(today);
      data.setDue(today);

      // ステータスを未着手にする
//      data.getStatusData().setStatusCd(0);
    }
    return data;
  }

  /**
   * スタータスの一覧を表示する.
   * @return
   */
  public Map<String, String> getStatusItem() {
    Map<String, String> statusItem = new LinkedHashMap<String, String>();

    List<StatusData> list = statusService.getAll();

    for (StatusData data : list) {
      String statusCd = String.valueOf(data.getStatusCd());
      String statusName = data.getStatusName();
      statusItem.put(statusCd,statusName);
    }
    return statusItem;
  }

}
