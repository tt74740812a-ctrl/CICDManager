ĐỒ ÁN JAVA: XÂY DỰNG HỆ THỐNG TỰ ĐỘNG HÓA KIỂM THỬ VÀ TRIỂN KHAI PHẦN MỀM

HƯỚNG DẪN CHẠY PHẦN MỀM CI/CD MANAGER
=====================================

YÊU CẦU
- Windows, đã cài Java JDK 11 trở lên.
- Chế độ DEMO không cần thêm gì. Chế độ REAL cần cài Maven và có Internet.

CÁCH CHẠY
1. Vào thư mục CICDManager_Release.
2. Nháy đúp vào file run.bat.
   (Nếu ứng dụng không mở được: nháy đúp run_debug.bat để xem thông báo lỗi.)
3. Đăng nhập bằng một trong hai tài khoản bên dưới.
4. Sau khi đăng nhập, chọn chức năng ở thanh bên trái:
   - Dashboard: xem tổng số dự án, pipeline, số lần chạy thành công và thất bại.
   - Quản lý dự án: thêm, sửa, xóa, tìm kiếm dự án.
   - Quản lý pipeline: tạo pipeline gồm các bước BUILD, TEST, DEPLOY;
     bật/tắt từng bước và sắp xếp thứ tự.
   - Chạy pipeline: chọn pipeline rồi nhấn "Chạy pipeline".
5. Theo dõi quá trình chạy: trạng thái (RUNNING, SUCCESS, FAILED),
   thanh tiến trình và nhật ký hiện trực tiếp. Muốn dừng thì nhấn "Dừng".
6. Xem lại kết quả ở mục "Lịch sử chạy" (có thống kê) và "Nhật ký (Log)"
   (tìm kiếm trong nội dung log, lưu log ra file .txt).
7. Muốn đổi mật khẩu: nhấn "Đổi mật khẩu" ở góc dưới thanh bên.
8. Khi dùng xong, nhấn "Đăng xuất" hoặc đóng cửa sổ.

TÀI KHOẢN ĐĂNG NHẬP
- Quản trị viên (Admin): được xem, thêm, sửa, xóa và chạy pipeline.
  + Tên đăng nhập: admin
  + Mật khẩu: Admin@123
- Người dùng (khách): chỉ được xem và chạy pipeline,
  không thêm, sửa, xóa được dự án và pipeline.
  + Tên đăng nhập: user
  + Mật khẩu: User@123

Sinh viên thực hiện:Vũ Tiến Dũng
Lớp:TH30.15