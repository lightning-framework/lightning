package lightning.mvc;

public class ModelAndView {
  public final String viewName;
  public final Object viewModel;
  
  public ModelAndView(String viewName, Object viewModel) {
    this.viewModel = viewModel;
    this.viewName = viewName;
  }
}
