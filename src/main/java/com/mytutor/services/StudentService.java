/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package com.mytutor.services;

import com.mytutor.constants.AccountStatus;
import com.mytutor.dto.QuestionDto;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Nguyen Van Dat
 */
public interface StudentService {

    ResponseEntity<?> getAllStudents(int pageNo, int pageSize, String status);
    
    ResponseEntity<?> getAllQuestion(int pageNo, int pageSize, String type, String subjects, String content);

    ResponseEntity<?> getQuestionById(Integer questionId);

    ResponseEntity<?> addQuestion(Integer studentId, QuestionDto questionDto);
    
    ResponseEntity<?> updateQuestion(Integer studentId, Integer questionId, QuestionDto questionDto);
    
    ResponseEntity<?> deleteQuestion(Integer studentId, Integer questionId);
}
