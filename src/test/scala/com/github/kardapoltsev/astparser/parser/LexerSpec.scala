/*
  Copyright 2016 Alexey Kardapoltsev

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.github.kardapoltsev.astparser.parser

import org.scalatest.{Matchers, WordSpec}


class LexerSpec extends WordSpec with Matchers {
  import Tokens._
  val lexer = new Lexer

  def scan(input: String): List[Token] = lexer.scan(input)

  def hasError(tokens: List[Token]): Boolean = tokens.exists {
    case Error(_) => true
    case _ => false
  }

  "Lexer" should {
    "should parse single lexeme" in {
      val in = "34asd2478sdfg"
      scan(in) shouldBe List(Lexeme(in))
    }

    "shouldn't parse invalid lexeme" in {
      val in = "__#$sd_fg"
      hasError(scan(in)) shouldBe true
    }

    "should parse symbols" in {
      val in = ";:#.{}[]="
      scan(in) shouldBe List(
        Semicolon(), Colon(), Hash(), Dot(), LeftBrace(), RightBrace(),
        LeftBracket(), RightBracket(), Eq()
      )
    }

    "shouldn't parse invalid symbols" in {
      val in = "asdf $a sdfadsf% *"
      hasError(scan(in)) shouldBe true
    }

    "should ignore line comments" in {
      val in =
        """
          |asdf
          |// asdfadsf
          |adsf
        """.stripMargin
      scan(in) shouldBe List(Lexeme("asdf"), Lexeme("adsf"))
    }

    "should ignore multiline comment" in {
      val in =
        """
          |asdf
          |/* asdfadsf
          |asdf
          |adsf
          |  */
          |adsf
        """.stripMargin
      scan(in) shouldBe List(Lexeme("asdf"), Lexeme("adsf"))
    }

    "should lead to the error if comment unclosed" in {
      val res = scan("/* asdf ")
      // println(res)
      hasError(res) shouldBe true
    }


    "should parse single line doc" in {
      val commentSample = " a - sdfads -- adfasdf "
      val in = s"asdf --$commentSample"
      scan(in) shouldBe List(Lexeme("asdf"), RightDoc(commentSample))
    }


    "should parse multiline doc" in {
      val commentSample =
        """
          |asf*
          |*a a
          |dsf*"""".stripMargin

      val in =
        s"""
          |asdf
          |/**$commentSample*/
          |asdf
        """.stripMargin

      scan(in) shouldBe List(Lexeme("asdf"), LeftDoc(commentSample), Lexeme("asdf"))
    }

    "should parse valid input" in {
      val multiDocSample =
        """
          | adfasdf
          | asd fadsf
        """.stripMargin
      val lineDocSample = "/ asdf // asdf -- asdf - adsf"

      val sample =
        List(
          Lexeme("ASD"), LeftBrace(), LeftDoc(multiDocSample),
          Lexeme("abc"), Dot(), Lexeme("abc"), Hash(), Lexeme("789"), Colon(),
          Lexeme("T"), LeftBracket(), Lexeme("B"), RightBracket(), Semicolon(),
          RightDoc(lineDocSample), RightBrace())

      val in =
        s"""
          |  ASD { // $lineDocSample
          |    /*$multiDocSample*/
          |    /**$multiDocSample*/
          |    abc.abc # 789 : T[B]; --$lineDocSample
          |  }
        """.stripMargin

      val res = scan(in)
      // println(res)
      res shouldBe sample
    }


    "should parse type aliases" in {
      val in =
        """
          |type MyType = some.other.Type;
          |type MyType2 = some.other.Type
        """.stripMargin

      scan(in) shouldBe List(
        TypeKeyword(), Lexeme("MyType"), Eq(),
        Lexeme("some"), Dot(), Lexeme("other"), Dot(), Lexeme("Type"), Semicolon(),
        TypeKeyword(), Lexeme("MyType2"), Eq(),
        Lexeme("some"), Dot(), Lexeme("other"), Dot(), Lexeme("Type")
      )
    }

    "should parse identifiers containing keywords" in {
      val in = "type typeAliasForX"
      scan(in) shouldBe List(
        TypeKeyword(), Lexeme("typeAliasForX")
      )
    }

    "should allow escaped keywords" in {
      scan("`type`") shouldBe List(
        Lexeme("type")
      )
    }


  }
}