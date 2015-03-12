namespace java edu.purdue.absoa
  
typedef i64 long
typedef i32 int
  
service ABService
{
	string getValue(1:string request, 2:string signature, 3:string certificate)
}